package com.vikram.expressiveisland.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File

/** Removes the downloaded installer once Android has replaced this app package. */
class UpdateCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "expressive-island-update.apk").delete()
        context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE).edit().clear().apply()
    }
}
