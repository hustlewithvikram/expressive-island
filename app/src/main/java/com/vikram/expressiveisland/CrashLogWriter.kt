package com.vikram.expressiveisland

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
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
 * Crash reports are written to shared storage at /storage/emulated/0/Expressive Island/logs,
 * using MediaStore so Android 10+ users can access them without broad storage permissions.
 * If shared storage is unavailable, a private app-storage fallback is used. Nothing is uploaded.
 */
object CrashLogWriter {
    private const val TAG = "ExpressiveIslandCrash"
    private const val DIRECTORY_NAME = "Expressive Island"
    private const val LOGS_DIRECTORY = "logs"

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

        // MediaStore writes to a user-visible shared folder without requesting
        // MANAGE_EXTERNAL_STORAGE. The resulting path is:
        // /storage/emulated/0/Expressive Island/logs/<timestamped report>
        val resolver = application.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$DIRECTORY_NAME/$LOGS_DIRECTORY")
        }
        val collection = MediaStore.Files.getContentUri("external")
        val sharedUri = runCatching { resolver.insert(collection, values) }.getOrNull()
        if (sharedUri != null) {
            val saved = runCatching {
                resolver.openFileDescriptor(sharedUri, "w")?.use { descriptor ->
                    FileOutputStream(descriptor.fileDescriptor).use { stream ->
                        stream.write(report.toByteArray(Charsets.UTF_8))
                        stream.flush()
                        stream.fd.sync()
                    }
                } ?: throw IllegalStateException("Unable to open shared crash-log file")
            }
            if (saved.isSuccess) {
                Log.e(TAG, "Crash report saved to /storage/emulated/0/$DIRECTORY_NAME/$LOGS_DIRECTORY/$fileName")
                return
            }
            runCatching { resolver.delete(sharedUri, null, null) }
            Log.e(TAG, "Shared crash-log write failed; using app-private fallback", saved.exceptionOrNull())
        }

        // Keep crash reporting reliable if shared storage is temporarily unavailable.
        val internalDirectory = File(application.filesDir, "expressive_island_logs")
        if (!internalDirectory.exists() && !internalDirectory.mkdirs()) {
            throw IllegalStateException("Unable to create fallback crash-log directory")
        }
        val file = File(internalDirectory, fileName)
        FileOutputStream(file, false).use { stream ->
            stream.write(report.toByteArray(Charsets.UTF_8))
            stream.flush()
            stream.fd.sync()
        }
        Log.e(TAG, "Crash report saved to fallback path: ${file.absolutePath}")
    }
}
