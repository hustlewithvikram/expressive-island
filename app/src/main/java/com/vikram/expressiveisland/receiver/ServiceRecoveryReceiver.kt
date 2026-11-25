package com.vikram.expressiveisland.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.vikram.expressiveisland.service.CutoutAccessibilityService
import com.vikram.expressiveisland.service.CutoutNotificationListenerService

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

        // AccessibilityService instances are rebound by Android itself when the user
        // has kept the service enabled. Unlike NotificationListenerService, there is no
        // public requestRebind API for AccessibilityService.

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
