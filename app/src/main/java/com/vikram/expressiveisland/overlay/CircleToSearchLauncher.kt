package com.vikram.expressiveisland.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/** MiCTS-compatible trigger for Google's native Circle to Search / Omni session. */
object CircleToSearchLauncher {
    private const val TAG = "CircleToSearch"

    @SuppressLint("PrivateApi")
    fun trigger(context: Context): Boolean = runCatching {
        val args = Bundle().apply {
            putLong("invocation_time_ms", SystemClock.elapsedRealtime())
            putInt("omni.entry_point", 1)
            putBoolean("micts_trigger", true)
        }
        val managerClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java).invoke(null, "voiceinteraction") as? IBinder
            ?: error("Voice interaction service unavailable")
        val manager = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
            .getMethod("asInterface", IBinder::class.java).invoke(null, binder)
            ?: error("Voice interaction manager unavailable")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HiddenApiBypass.invoke(managerClass, manager, "showSessionFromSession", null, args, 7, "hyperOS_home") as? Boolean ?: false
        } else {
            HiddenApiBypass.invoke(managerClass, manager, "showSessionFromSession", null, args, 7) as? Boolean ?: false
        }
    }.onFailure { Log.w(TAG, "Circle to Search trigger failed", it) }.getOrDefault(false)
}
