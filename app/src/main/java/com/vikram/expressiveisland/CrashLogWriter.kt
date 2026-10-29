package com.vikram.expressiveisland

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persists uncaught exceptions before Android's original handler terminates the process.
 *
 * Files are written to the app-specific external folder
 * Android/data/<package>/files/expressive_island_logs, with an internal-storage fallback.
 * No storage permission is required and no crash data is uploaded.
 */
object CrashLogWriter {
    private const val TAG = "ExpressiveIslandCrash"
    private const val DIRECTORY_NAME = "expressive_island_logs"

    @Volatile
    private var installed = false

    @Synchronized
    fun install(application: Application) {
        if (installed) return
        installed = true

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Keep the write synchronous: the process is about to terminate.
            runCatching { writeCrashLog(application, thread, throwable) }
                .onFailure { Log.e(TAG, "Could not persist crash log", it) }

            // Preserve Android's normal crash reporting and process termination.
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    private fun writeCrashLog(application: Application, thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "expressive_island_logs_$timestamp.txt"
        val externalDirectory = application.getExternalFilesDir(DIRECTORY_NAME)
        val internalDirectory = File(application.filesDir, DIRECTORY_NAME)
        val directory = externalDirectory?.takeIf { it.exists() || it.mkdirs() }
            ?: internalDirectory.apply { if (!exists()) mkdirs() }
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Unable to create crash-log directory")
        }

        val stackTrace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()

        val report = buildString {
            appendLine("Expressive Island crash report")
            appendLine("Timestamp: $timestamp")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Process: ${application.applicationInfo.processName}")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine()
            appendLine("Uncaught exception:")
            append(stackTrace)
        }

        val file = File(directory, fileName)
        FileOutputStream(file, false).use { stream ->
            stream.write(report.toByteArray(Charsets.UTF_8))
            stream.flush()
            stream.fd.sync()
        }
        Log.e(TAG, "Crash report saved to ${file.absolutePath}")
    }
}
