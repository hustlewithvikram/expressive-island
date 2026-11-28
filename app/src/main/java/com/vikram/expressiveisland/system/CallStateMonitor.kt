package com.vikram.expressiveisland.system

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.content.ContextCompat

/**
 * Optional platform call-state observer. It only tracks whether the default dialer's cellular call
 * is ringing, active or idle; it never reads phone numbers, contacts or call history.
 *
 * VoIP calls are not represented by TelephonyManager, so callers must retain notification-based
 * cleanup as a fallback for apps other than the default dialer.
 */
class CallStateMonitor(private val context: Context) {
    private var listener: Any? = null
    private var lastState: Int? = null

    val isIdle: Boolean get() = lastState == TelephonyManager.CALL_STATE_IDLE

    fun start(onCallEnded: () -> Unit): Boolean {
        if (listener != null) return true
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE,
            ) != PackageManager.PERMISSION_GRANTED
        ) return false

        val telephony = context.getSystemService<TelephonyManager>() ?: return false
        val registered: Any = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = onState(state, onCallEnded)
            }
        } else {
            @Suppress("DEPRECATION")
            object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                    onState(state, onCallEnded)
            }
        }

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephony.registerTelephonyCallback(context.mainExecutor, registered as TelephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                telephony.listen(registered as PhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
            listener = registered
        }.onFailure { Log.w(TAG, "Call state monitoring unavailable; using notification fallback", it) }
            .isSuccess
    }

    fun stop() {
        val registered = listener ?: return
        listener = null
        lastState = null
        val telephony = context.getSystemService<TelephonyManager>() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephony.unregisterTelephonyCallback(registered as TelephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                telephony.listen(registered as PhoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
        }.onFailure { Log.w(TAG, "Failed to unregister call-state listener", it) }
    }

    private fun onState(state: Int, onCallEnded: () -> Unit) {
        lastState = state
        if (state == TelephonyManager.CALL_STATE_IDLE) onCallEnded()
    }

    companion object {
        private const val TAG = "CallStateMonitor"

        fun knowsAbout(context: Context, packageName: String?): Boolean {
            if (packageName == null) return false
            val dialer = runCatching {
                context.getSystemService<TelecomManager>()?.defaultDialerPackage
            }.getOrNull() ?: return false
            return dialer == packageName
        }
    }
}
