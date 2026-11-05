package com.vikram.expressiveisland.system

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import androidx.annotation.Keep
import java.util.concurrent.TimeUnit

/**
 * Shizuku UserService running with shell privileges. Only exposes fixed radio-toggle operations;
 * callers cannot submit arbitrary shell commands. This avoids the deprecated private newProcess API.
 */
@Keep
class ShizukuCommandService : Binder() {

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == TRANSACTION_TOGGLE_RADIO) {
            data.enforceInterface(DESCRIPTOR)
            val radioId = data.readInt()
            val result = runCatching { toggleRadio(radioId) }.getOrDefault(RESULT_FAILED)
            reply?.writeNoException()
            reply?.writeString(result)
            return true
        }

        if (code == TRANSACTION_DESTROY) {
            Thread { System.exit(0) }.start()
            return true
        }

        return super.onTransact(code, data, reply, flags)
    }

    private fun toggleRadio(radioId: Int): String {
        val (service, setting) = when (radioId) {
            RADIO_WIFI -> "wifi" to "wifi_on"
            RADIO_BLUETOOTH -> "bluetooth" to "bluetooth_on"
            RADIO_MOBILE_DATA -> "data" to "mobile_data"
            else -> return RESULT_FAILED
        }

        val current = runShell("settings get global $setting") ?: return RESULT_STATE_UNKNOWN
        if (current != "0" && current != "1") return RESULT_STATE_UNKNOWN

        val wasEnabled = current == "1"
        val verb = if (wasEnabled) "disable" else "enable"
        val expected = if (wasEnabled) "0" else "1"
        val updated = runShell(
            "svc $service $verb >/dev/null 2>&1; sleep 1; settings get global $setting"
        )

        return if (updated == expected) RESULT_OK else RESULT_FAILED
    }

    private fun runShell(command: String): String? = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (process.exitValue() == 0) output else null
    }.getOrNull()

    companion object {
        const val DESCRIPTOR = "com.vikram.expressiveisland.system.IShizukuCommandService"
        const val TRANSACTION_TOGGLE_RADIO = IBinder.FIRST_CALL_TRANSACTION
        const val TRANSACTION_DESTROY = 16777115

        const val RADIO_WIFI = 0
        const val RADIO_BLUETOOTH = 1
        const val RADIO_MOBILE_DATA = 2

        const val RESULT_OK = "OK"
        const val RESULT_STATE_UNKNOWN = "STATE_UNKNOWN"
        const val RESULT_FAILED = "FAILED"

        private const val COMMAND_TIMEOUT_SECONDS = 6L
    }
}
