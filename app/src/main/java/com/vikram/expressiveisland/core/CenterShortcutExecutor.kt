package com.vikram.expressiveisland.core

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.CenterShortcut
import com.vikram.expressiveisland.data.CameraHoleTapAction
import com.vikram.expressiveisland.data.GlobalAction
import com.vikram.expressiveisland.data.RadioToggle
import com.vikram.expressiveisland.service.CutoutAccessibilityService
import com.vikram.expressiveisland.system.ShizukuCommandService
import com.vikram.expressiveisland.system.ShizukuState
import com.vikram.expressiveisland.system.ShizukuStatus
import android.widget.Toast
import rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Performs a [CenterShortcut]. Global actions go through the live accessibility service
 * ([CutoutAccessibilityService.Companion.performGlobal]); torch and app launches use platform APIs. Every call is best-effort and swallows failures, so a dead service, a device without
 * a flash, or a missing app never crashes the overlay — it just returns false.
 */
object CenterShortcutExecutor {

    // Hosts the brief torch-state read only; no work — and no camera listener — lives here at rest.
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // How long to wait for the camera service to report the current torch state before giving up
    // (e.g. the flash is busy in another app). Kept tight — the state callback is near-immediate.
    private const val TORCH_STATE_TIMEOUT_MS = 500L

    /** Runs the shortcut. Returns true if it could be dispatched; false means it isn't available here. */
    fun execute(shortcut: CenterShortcut, context: Context): Boolean = when (shortcut) {
        is CenterShortcut.Global -> performGlobal(shortcut)
        CenterShortcut.Torch -> toggleTorch(context)
        is CenterShortcut.Panel -> startIntent(Intent(shortcut.panel.action), context)
        CenterShortcut.Assistant -> launchDeviceAssistant(context)
        is CenterShortcut.LaunchApp -> launchApp(shortcut.packageName, context)
    }

    /**
     * Radio controls are exposed only through an explicitly granted Shizuku shell session.
     * No Settings-page fallback is opened. State is read before toggling and verified afterwards;
     * unsupported OEM command behaviour is reported instead of pretending the toggle worked.
     */
    fun executeCameraHoleAction(action: CameraHoleTapAction, context: Context): Boolean = when (action) {
        CameraHoleTapAction.NONE -> false
        CameraHoleTapAction.QUICK_ACTIONS -> false // Opened by the overlay controller.
        CameraHoleTapAction.ASSISTANT -> launchDeviceAssistant(context)
        CameraHoleTapAction.APP_ASSISTANT -> false // Opened by the overlay controller's assistant tile.
        CameraHoleTapAction.SCREENSHOT -> execute(CenterShortcut.Global(GlobalAction.SCREENSHOT), context)
        CameraHoleTapAction.LOCK_SCREEN -> execute(CenterShortcut.Global(GlobalAction.LOCK_SCREEN), context)
        CameraHoleTapAction.POWER_MENU -> execute(CenterShortcut.Global(GlobalAction.POWER_DIALOG), context)
        CameraHoleTapAction.NOTIFICATIONS -> execute(CenterShortcut.Global(GlobalAction.NOTIFICATIONS), context)
        CameraHoleTapAction.QUICK_SETTINGS -> execute(CenterShortcut.Global(GlobalAction.QUICK_SETTINGS), context)
        CameraHoleTapAction.RECENTS -> execute(CenterShortcut.Global(GlobalAction.RECENTS), context)
        CameraHoleTapAction.WIFI -> executeRadioToggle(RadioToggle.WIFI, context)
        CameraHoleTapAction.BLUETOOTH -> executeRadioToggle(RadioToggle.BLUETOOTH, context)
        CameraHoleTapAction.MOBILE_DATA -> executeRadioToggle(RadioToggle.MOBILE_DATA, context)
        CameraHoleTapAction.FLASHLIGHT -> execute(CenterShortcut.Torch, context)
    }

    fun executeRadioToggle(radio: RadioToggle, context: Context): Boolean =
        toggleShizukuRadio(radio, context)

    private fun toggleShizukuRadio(radio: RadioToggle, context: Context): Boolean {
        ShizukuState.refresh()
        val status = ShizukuState.status.value
        if (status != ShizukuStatus.READY) {
            if (status == ShizukuStatus.PERMISSION_REQUIRED) {
                ShizukuState.requestPermission()
            }
            scope.launch {
                Toast.makeText(context, R.string.center_shizuku_required, Toast.LENGTH_SHORT).show()
            }
            return false
        }

        scope.launch(Dispatchers.IO) {
            val result = runShizukuToggle(context, radio)
            val message = when (result) {
                ShizukuCommandService.RESULT_OK -> R.string.center_shizuku_toggle_success
                ShizukuCommandService.RESULT_STATE_UNKNOWN -> R.string.center_shizuku_state_unavailable
                else -> R.string.center_shizuku_toggle_failed
            }
            scope.launch {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    /**
     * Uses Shizuku's supported UserService API rather than the deprecated private newProcess bridge.
     * The remote service accepts only a radio enum, never an arbitrary shell command.
     */
    private suspend fun runShizukuToggle(context: Context, radio: RadioToggle): String? {
        val args = Shizuku.UserServiceArgs(
            ComponentName(context, ShizukuCommandService::class.java),
        )
            .processNameSuffix("radio_actions")
            .tag("expressive-island-radio-actions")
            .version(1)
            .daemon(false)

        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean(false)
                lateinit var connection: ServiceConnection

                fun finish(result: String?) {
                    if (completed.compareAndSet(false, true)) {
                        if (continuation.isActive) continuation.resume(result)
                        runCatching { Shizuku.unbindUserService(args, connection, true) }
                    }
                }

                connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        scope.launch(Dispatchers.IO) {
                            val result = runCatching {
                                val data = Parcel.obtain()
                                val reply = Parcel.obtain()
                                try {
                                    data.writeInterfaceToken(ShizukuCommandService.DESCRIPTOR)
                                    data.writeInt(
                                        when (radio) {
                                            RadioToggle.WIFI -> ShizukuCommandService.RADIO_WIFI
                                            RadioToggle.BLUETOOTH -> ShizukuCommandService.RADIO_BLUETOOTH
                                            RadioToggle.MOBILE_DATA -> ShizukuCommandService.RADIO_MOBILE_DATA
                                        },
                                    )
                                    check(
                                        requireNotNull(service).transact(
                                            ShizukuCommandService.TRANSACTION_TOGGLE_RADIO,
                                            data,
                                            reply,
                                            0,
                                        ),
                                    )
                                    reply.readException()
                                    reply.readString()
                                } finally {
                                    data.recycle()
                                    reply.recycle()
                                }
                            }.getOrNull()
                            finish(result)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        finish(null)
                    }
                }

                runCatching { Shizuku.bindUserService(args, connection) }
                    .onFailure { finish(null) }
                continuation.invokeOnCancellation { finish(null) }
            }
        }
    }

    private fun performGlobal(shortcut: CenterShortcut.Global): Boolean {
        if (Build.VERSION.SDK_INT < shortcut.action.minSdk) return false
        return CutoutAccessibilityService.Companion.performGlobal(shortcut.action.action)
    }

    private fun launchApp(packageName: String, context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return startIntent(launch, context)
    }

    /** Launch the device's configured assistant rather than the app's internal assistant panel. */
    private fun launchDeviceAssistant(context: Context): Boolean {
        if (startIntent(Intent(Intent.ACTION_ASSIST), context)) return true
        return startIntent(Intent(Intent.ACTION_VOICE_COMMAND), context)
    }

    private fun startIntent(intent: Intent, context: Context): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    /**
     * Publish the current torch state into [TorchStateBus] without changing it — called when the
     * center opens so the torch shortcut renders lit/unlit correctly from the first frame.
     */
    fun syncTorchState(context: Context) {
        val cameraManager = context.getSystemService<CameraManager>() ?: return
        scope.launch {
            val (_, isOn) = readTorchState(context, cameraManager) ?: return@launch
            TorchStateBus.update(isOn)
        }
    }

    /**
     * Toggle the camera flash. Rather than keep a resident [CameraManager.TorchCallback] alive
     * between taps, we register one only long enough to read the authoritative current state — the
     * camera service reports it near-immediately on registration — unregister it, then flip. A
     * toggle is therefore always correct even if another app changed the torch, with no listeners at
     * rest. Returns true optimistically (the flip runs asynchronously once the state is read).
     */
    private fun toggleTorch(context: Context): Boolean {
        val cameraManager = context.getSystemService<CameraManager>() ?: return false
        scope.launch {
            val (cameraId, isOn) = readTorchState(context, cameraManager) ?: return@launch
            val next = !isOn
            if (runCatching { cameraManager.setTorchMode(cameraId, next) }.isSuccess) {
                TorchStateBus.update(next)
            }
        }
        return true
    }

    /**
     * The current (cameraId, enabled) of the first flash-capable camera, read via a one-shot
     * [CameraManager.TorchCallback] that is unregistered the instant it reports — and on timeout or
     * cancellation via [kotlinx.coroutines.CancellableContinuation.invokeOnCancellation], so it never
     * outlives the read. Null when no flash reports a state in time (e.g. none present, or in use).
     */
    private suspend fun readTorchState(
        context: Context,
        cameraManager: CameraManager,
    ): Pair<String, Boolean>? = withTimeoutOrNull(TORCH_STATE_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    runCatching { cameraManager.unregisterTorchCallback(this) }
                    if (continuation.isActive) continuation.resume(cameraId to enabled)
                }
            }
            cameraManager.registerTorchCallback(context.mainExecutor, callback)
            continuation.invokeOnCancellation {
                runCatching { cameraManager.unregisterTorchCallback(callback) }
            }
        }
    }
}
