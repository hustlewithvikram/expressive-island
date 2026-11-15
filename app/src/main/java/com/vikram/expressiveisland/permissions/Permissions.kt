package com.vikram.expressiveisland.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.service.CutoutAccessibilityService
import com.vikram.expressiveisland.system.ShizukuState

/**
 * Thin, side-effect-free helpers for querying and requesting the three grants the app
 * needs. Keeping the "is it on?" checks and the "open the right screen" intents together
 * lets the UI stay declarative and re-check state on every resume.
 */
object Permissions {

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

    fun openNotificationAccessSettings(context: Context) =
        context.startActivitySafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openAccessibilitySettings(context: Context) =
        context.startActivitySafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

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
