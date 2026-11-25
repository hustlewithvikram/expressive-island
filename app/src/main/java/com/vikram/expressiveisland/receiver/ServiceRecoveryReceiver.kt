package com.vikram.expressiveisland.receiver

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.vikram.expressiveisland.service.CutoutAccessibilityService
import com.vikram.expressiveisland.service.CutoutNotificationListenerService
import android.provider.Settings

/**
 * Requests the system-bound services to reconnect after a reboot or app replacement.
 *
 * Android owns the lifecycle of AccessibilityService and NotificationListenerService;
 * this receiver cannot (and must not) silently enable either permission. It only asks
 * Android to rebind services whose corresponding user-granted access is still enabled.
 */
class ServiceRecoveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> recoverSystemServices(context)
        }
    }

    private fun recoverSystemServices(context: Context) {
        val packageName = context.packageName

        val enabledAccessibilityServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val accessibilityComponent = ComponentName(
            context,
            CutoutAccessibilityService::class.java,
        ).flattenToString()
        val accessibilityEnabled = enabledAccessibilityServices
            .split(':')
            .any { it.equals(accessibilityComponent, ignoreCase = true) }

        if (accessibilityEnabled) {
            runCatching {
                AccessibilityService.requestRebind(
                    ComponentName(context, CutoutAccessibilityService::class.java),
                )
            }.onFailure {
                Log.w(TAG, "Could not request accessibility service rebind", it)
            }
        }

        val notificationAccessEnabled =
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(packageName)
        if (notificationAccessEnabled) {
            runCatching {
                NotificationListenerService.requestRebind(
                    ComponentName(context, CutoutNotificationListenerService::class.java),
                )
            }.onFailure {
                Log.w(TAG, "Could not request notification listener rebind", it)
            }
        }
    }

    private companion object {
        const val TAG = "IslandServiceRecovery"
    }
}
