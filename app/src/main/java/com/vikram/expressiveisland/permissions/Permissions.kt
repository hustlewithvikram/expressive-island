package com.vikram.expressiveisland.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.service.CutoutAccessibilityService
import com.vikram.expressiveisland.system.ShizukuState
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import com.vikram.expressiveisland.system.ShizukuCommandService
import com.vikram.expressiveisland.system.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

/**
 * Thin, side-effect-free helpers for querying and requesting the three grants the app
 * needs. Keeping the "is it on?" checks and the "open the right screen" intents together
 * lets the UI stay declarative and re-check state on every resume.
 */
object Permissions {


    /**
     * Wi-Fi SSID access is location-sensitive on legacy Android releases. Android 13+
     * additionally exposes the Nearby Wi-Fi Devices runtime permission.
     */
    fun isWifiSsidAccessGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val nearbyWifi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || nearbyWifi
    }

    fun isLocationEnabledForWifi(context: Context): Boolean {
        // Nearby Wi-Fi permission is intentionally independent of device location on Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        ) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val manager = context.getSystemService<android.location.LocationManager>() ?: return false
            return runCatching { manager.isLocationEnabled }.getOrDefault(false)
        }

        // Android 6.0–8.1: location mode is required by the legacy Wi-Fi SSID APIs.
        return runCatching {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF,
            ) != Settings.Secure.LOCATION_MODE_OFF
        }.getOrDefault(false)
    }

    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    fun isAccessibilityGranted(context: Context): Boolean {
        val expected = ComponentName(context, CutoutAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService<PowerManager>() ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }


    /**
     * Uses an explicitly authorized Shizuku shell UserService to enable the core notification
     * listener and accessibility service. No permission is granted unless the user has already
     * authorized this app in Shizuku.
     */
    suspend fun grantRequiredWithShizuku(context: Context): Boolean {
        if (ShizukuState.status.value != ShizukuStatus.READY) return false

        val args = Shizuku.UserServiceArgs(
            ComponentName(context, ShizukuCommandService::class.java),
        ).daemon(false)
            .tag("expressive-island-permission-grant")
            .version(1)

        lateinit var connection: ServiceConnection
        val binder = runCatching {
            withTimeoutOrNull(15_000L) {
                suspendCancellableCoroutine<IBinder> { continuation ->
                connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        if (continuation.isActive) continuation.resume(service)
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        if (continuation.isActive) {
                            continuation.resumeWith(
                                Result.failure(IllegalStateException("Shizuku service disconnected")),
                            )
                        }
                    }
                }
                continuation.invokeOnCancellation {
                    runCatching { Shizuku.unbindUserService(args, connection, true) }
                }
                runCatching {
                    Shizuku.bindUserService(args, connection)
                }.onFailure { continuation.resumeWith(Result.failure(it)) }
                }
            }
        }.getOrNull() ?: return false

        return try {
            withContext(Dispatchers.IO) {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(ShizukuCommandService.DESCRIPTOR)
                    data.writeString(context.packageName)
                    data.writeString(
                        ComponentName(
                            context,
                            com.vikram.expressiveisland.service.CutoutNotificationListenerService::class.java,
                        ).flattenToString(),
                    )
                    data.writeString(
                        ComponentName(
                            context,
                            com.vikram.expressiveisland.service.CutoutAccessibilityService::class.java,
                        ).flattenToString(),
                    )
                    binder.transact(
                        ShizukuCommandService.TRANSACTION_GRANT_REQUIRED_PERMISSIONS,
                        data,
                        reply,
                        0,
                    )
                    reply.readException()
                    reply.readInt() == 1
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }
        } catch (_: Exception) {
            false
        } finally {
            runCatching { Shizuku.unbindUserService(args, connection, true) }
            ShizukuState.refresh()
        }
    }

    fun openNotificationAccessSettings(context: Context) =
        context.startActivitySafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openAccessibilitySettings(context: Context) =
        context.startActivitySafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    /**
     * Opens the direct "ignore battery optimisation" prompt for this app. Falls back to
     * the general list if the OEM blocks the targeted request.
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        val targeted = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        if (!context.startActivitySafely(targeted)) {
            context.startActivitySafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    /**
     * Opens the installed Shizuku manager using the package that declares its stable API
     * permission. This also works when Shizuku Stealth Mode changes the manager package ID.
     * Falls back to the manager's app-details page if it has no launchable activity, and to
     * the Play Store only when no Shizuku installation can be detected.
     */
    fun openShizuku(context: Context) {
        val packageName = ShizukuState.getShizukuPackageName(context)
        if (packageName != null) {
            val launch = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null && context.startActivitySafely(launch)) return

            val details = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            )
            if (context.startActivitySafely(details)) return
        }

        context.startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_STORE_URL)))
    }

    private fun Context.startActivitySafely(intent: Intent): Boolean = runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    private const val SHIZUKU_STORE_URL =
        "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"
}
