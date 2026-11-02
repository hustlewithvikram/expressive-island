package com.vikram.expressiveisland

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Speech engine is owned by the app process, not by a voice-assistant UI activity. */
object AssistantSpeechController : RecognitionListener {
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()
    private var recognizer: SpeechRecognizer? = null
    private var appContext: Context? = null

    fun start(context: Context): Boolean {
        if (_isListening.value) return true
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            !SpeechRecognizer.isOnDeviceRecognitionAvailable(app)) return false
        return runCatching {
            appContext = app
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(app).also {
                it.setRecognitionListener(this)
            }
            _isListening.value = true
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
            }
            recognizer?.startListening(request)
            true
        }.getOrElse {
            finishRecognition()
            false
        }
    }

    fun stopListening() {
        if (_isListening.value) runCatching { recognizer?.stopListening() }
    }

    private fun finishRecognition() {
        _isListening.value = false
        recognizer?.destroy()
        recognizer = null
        appContext = null
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onError(error: Int) { finishRecognition() }
    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { AssistantSpeechResultBus.results.tryEmit(it) }
        finishRecognition()
    }
    override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { AssistantSpeechResultBus.results.tryEmit(it) }
    }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

object AssistantSpeechResultBus {
    val results = MutableSharedFlow<String>(extraBufferCapacity = 1)
}

/** Brief permission trampoline; it exits immediately so the overlay remains touchable while listening. */
class AssistantSpeechActivity : ComponentActivity() {
    private val permissionRequestCode = 7143

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginRecognition()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), permissionRequestCode)
        }
    }

    private fun beginRecognition() {
        if (!AssistantSpeechController.start(applicationContext)) {
            Toast.makeText(this, "On-device speech recognition is unavailable on this device.", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            beginRecognition()
        } else {
            finish()
        }
    }
}
