package com.vikram.expressiveisland

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Locale

/** Delivers speech recognition results back to the live assistant composer in the overlay. */
object AssistantSpeechResultBus {
    val results = MutableSharedFlow<String>(extraBufferCapacity = 1)
}

class AssistantSpeechActivity : ComponentActivity() {
    private val requestCode = 7142

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening… Speak your message")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (_: Exception) {
            finish()
        }
    }

    @Deprecated("Uses the platform speech-recognition activity result contract.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { AssistantSpeechResultBus.results.tryEmit(it) }
        }
        finish()
    }
}
