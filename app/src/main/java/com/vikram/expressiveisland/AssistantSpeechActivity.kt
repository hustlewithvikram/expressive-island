package com.vikram.expressiveisland

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** In-app speech capture state and transcript events for the assistant overlay. */
object AssistantSpeechController {
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()
    @Volatile private var stopAction: (() -> Unit)? = null
    internal fun setListening(value: Boolean) { _isListening.value = value }
    internal fun attachStop(action: (() -> Unit)?) { stopAction = action }
    fun stopListening() { stopAction?.invoke() }
}

object AssistantSpeechResultBus {
    val results = MutableSharedFlow<String>(extraBufferCapacity = 1)
}

class AssistantSpeechActivity : ComponentActivity(), RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var finalTranscript = ""
    private var finishing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            finishCapture()
            return
        }
        recognizer = if (SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }.also { it.setRecognitionListener(this) }
        AssistantSpeechController.attachStop { runOnUiThread { stopCapture() } }
        AssistantSpeechController.setListening(true)
        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
        }
        runCatching { recognizer?.startListening(request) }.onFailure { finishCapture() }
    }

    private fun stopCapture() {
        if (!finishing) runCatching { recognizer?.stopListening() }
    }

    private fun finishCapture() {
        if (finishing) return
        finishing = true
        AssistantSpeechController.setListening(false)
        AssistantSpeechController.attachStop(null)
        recognizer?.destroy()
        recognizer = null
        if (finalTranscript.isNotBlank()) AssistantSpeechResultBus.results.tryEmit(finalTranscript.trim())
        finish()
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onError(error: Int) { finishCapture() }
    override fun onResults(results: Bundle?) {
        finalTranscript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        finishCapture()
    }
    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!partial.isNullOrBlank()) finalTranscript = partial
    }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    override fun onDestroy() {
        AssistantSpeechController.setListening(false)
        AssistantSpeechController.attachStop(null)
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }
}
