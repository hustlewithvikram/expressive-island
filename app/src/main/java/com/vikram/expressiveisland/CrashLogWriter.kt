package com.vikram.expressiveisland

import android.app.Application
import android.content.ContentValues
import android.net.Uri
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
 * Creates a user-visible diagnostic log for each app process and appends uncaught exceptions.
 *
 * Shared location: Internal storage/Expressive Island/logs/
 * A private app-storage fallback is retained if shared storage cannot be written.
 * This logger does not upload anything or capture Android's entire system logcat.
 */
object CrashLogWriter {
    private const val TAG = "ExpressiveIslandCrash"
    private const val DIRECTORY_NAME = "Expressive Island"
    private const val LOGS_DIRECTORY = "logs"

    @Volatile
    private var installed = false

    @Volatile
    private var sharedLogUri: Uri? = null

    @Volatile
    private var fallbackLogFile: File? = null

    @Synchronized
    fun install(application: Application) {
        if (installed) return

        // Set the handler first so uncaught exceptions are handled even if log-file creation fails.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                append(
                    application,
                    buildString {
                        appendLine()
                        appendLine("========== UNCAUGHT EXCEPTION ==========")
                        appendLine("Thread: ${thread.name} (id=${thread.id})")
                        appendLine("Timestamp: ${timestamp()}")
                        appendLine(throwable.stackTraceText())
                    },
                )
            }.onFailure { Log.e(TAG, "Could not persist crash log", it) }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
        installed = true

        // Always create a session log on process startup, not only after a crash.
        runCatching {
            createSessionLog(application)
            append(
                application,
                buildString {
                    appendLine("Expressive Island diagnostic log")
                    appendLine("Started: ${timestamp()}")
                    appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Process: ${application.applicationInfo.processName}")
                    appendLine("Event: application process started")
                    appendLine("----------------------------------------")
                },
            )
        }.onFailure {
            Log.e(TAG, "Could not create diagnostic log", it)
        }
    }

    /** Records an explicit app diagnostic event in the current session log. */
    @Synchronized
    fun logEvent(application: Application, message: String, throwable: Throwable? = null) {
        runCatching {
            val details = buildString {
                append("[${timestamp()}] ")
                appendLine(message)
                if (throwable != null) appendLine(throwable.stackTraceText())
            }
            append(application, details)
        }.onFailure { Log.e(TAG, "Could not append diagnostic event", it) }
    }

    @Synchronized
    private fun createSessionLog(application: Application) {
        sharedLogUri = null
        fallbackLogFile = null
        val fileName = "expressive_island_logs_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}.txt"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$DIRECTORY_NAME/$LOGS_DIRECTORY")
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        val collection = MediaStore.Files.getContentUri("external")
        val uri = runCatching { application.contentResolver.insert(collection, values) }
            .onFailure { Log.e(TAG, "MediaStore insert failed", it) }
            .getOrNull()

        if (uri != null) {
            val opened = runCatching {
                application.contentResolver.openOutputStream(uri, "wa")?.use { stream ->
                    stream.write("Expressive Island log created.\n".toByteArray(Charsets.UTF_8))
                    stream.flush()
                } ?: throw IllegalStateException("MediaStore returned no output stream")
            }
            if (opened.isSuccess) {
                sharedLogUri = uri
                Log.i(TAG, "Diagnostic log created: $uri")
                return
            }
            Log.e(TAG, "MediaStore log initialization failed", opened.exceptionOrNull())
            runCatching { application.contentResolver.delete(uri, null, null) }
        }

        // Fallback remains private to the app, but preserves diagnostics if shared storage fails.
        val directory = File(application.filesDir, "expressive_island_logs")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Unable to create fallback log directory: ${directory.absolutePath}")
        }
        fallbackLogFile = File(directory, fileName).also { file ->
            FileOutputStream(file, true).use { stream ->
                stream.write("Expressive Island log created.\n".toByteArray(Charsets.UTF_8))
                stream.flush()
                stream.fd.sync()
            }
        }
        Log.w(TAG, "Diagnostic log created in app-private fallback: ${fallbackLogFile?.absolutePath}")
    }

    @Synchronized
    private fun append(application: Application, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val uri = sharedLogUri
        if (uri != null) {
            val result = runCatching {
                application.contentResolver.openOutputStream(uri, "wa")?.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                } ?: throw IllegalStateException("Unable to open shared log for append")
            }
            if (result.isSuccess) return
            Log.e(TAG, "Appending to shared log failed; switching to private fallback", result.exceptionOrNull())
            sharedLogUri = null
        }

        val file = fallbackLogFile ?: run {
            val directory = File(application.filesDir, "expressive_island_logs")
            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException("Unable to create fallback log directory")
            }
            File(directory, "expressive_island_logs_fallback.txt").also { fallbackLogFile = it }
        }
        FileOutputStream(file, true).use { stream ->
            stream.write(bytes)
            stream.flush()
            stream.fd.sync()
        }
        Log.i(TAG, "Diagnostic log appended to fallback: ${file.absolutePath}")
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    private fun Throwable.stackTraceText(): String =
        StringWriter().also { writer -> printStackTrace(PrintWriter(writer)) }.toString()
}
