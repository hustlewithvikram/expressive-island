package com.vikram.expressiveisland.core

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.data.CenterShortcut
import com.vikram.expressiveisland.data.RadioToggle
import com.vikram.expressiveisland.service.CutoutAccessibilityService
import com.vikram.expressiveisland.system.ShizukuState
import com.vikram.expressiveisland.system.ShizukuStatus
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Performs a [CenterShortcut]. Global actions go through the live accessibility service
 * ([CutoutAccessibilityService.Companion.performGlobal]); torch, settings panels and app launches use plain
 * platform APIs. Every call is best-effort and swallows failures, so a dead service, a device without
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
        is CenterShortcut.Panel -> false // Settings panels are intentionally not offered as quick actions.
        is CenterShortcut.ShizukuRadio -> toggleShizukuRadio(shortcut.radio, context)
        is CenterShortcut.LaunchApp -> launchApp(shortcut.packageName, context)
    }

    /**
     * Radio controls are exposed only through an explicitly granted Shizuku shell session.
     * No Settings-page fallback is opened. State is read before toggling and verified afterwards;
     * unsupported OEM command behaviour is reported instead of pretending the toggle worked.
     */
    private fun toggleShizukuRadio(radio: RadioToggle, context: Context): Boolean {
        ShizukuState.refresh()
        if (ShizukuState.status.value != ShizukuStatus.READY) {
            scope.launch {
                Toast.makeText(context, R.string.center_shizuku_required, Toast.LENGTH_SHORT).show()
            }
            return false
        }

        scope.launch(Dispatchers.IO) {
            val key = when (radio) {
                RadioToggle.WIFI -> "wifi_on"
                RadioToggle.BLUETOOTH -> "bluetooth_on"
                RadioToggle.MOBILE_DATA -> "mobile_data"
            }
            val state = runShizukuShell("settings get global $key")?.trim()
            if (state != "0" && state != "1") {
                scope.launch {
                    Toast.makeText(context, R.string.center_shizuku_state_unavailable, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val wasEnabled = state == "1"
            val command = when (radio) {
                RadioToggle.WIFI -> "svc wifi \${if (wasEnabled) "disable" else "enable"}"
                RadioToggle.BLUETOOTH -> "svc bluetooth \${if (wasEnabled) "disable" else "enable"}"
                RadioToggle.MOBILE_DATA -> "svc data \${if (wasEnabled) "disable" else "enable"}"
            }
            val output = runShizukuShell("$command >/dev/null 2>&1; sleep 1; settings get global $key")?.trim()
            val expected = if (wasEnabled) "0" else "1"
            scope.launch {
                val message = if (output == expected) {
                    R.string.center_shizuku_toggle_success
                } else {
                    R.string.center_shizuku_toggle_failed
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    /**
     * Executes a fixed, app-authored shell command through Shizuku's remote process bridge.
     * The command surface is deliberately not user-editable. Reflection isolates the deprecated
     * newProcess entry point so it can be replaced with UserService without changing callers.
     */
    private fun runShizukuShell(command: String): String? = runCatching {
        check(Shizuku.pingBinder()) { "Shizuku binder is unavailable" }
        check(Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            "Shizuku permission is missing"
        }
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        ).apply { isAccessible = true }
        val process = method.invoke(null, arrayOf("sh", "-c", "$command 2>&1"), null, null) as Process
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (process.exitValue() == 0) output else null
    }.getOrNull()

    private fun performGlobal(shortcut: CenterShortcut.Global): Boolean {
        if (Build.VERSION.SDK_INT < shortcut.action.minSdk) return false
        return CutoutAccessibilityService.Companion.performGlobal(shortcut.action.action)
    }

    private fun launchApp(packageName: String, context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return startIntent(launch, context)
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
