package com.vikram.expressiveisland.core

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live in-call audio controls for the expanded phone tile.
 *
 * State is refreshed from AudioManager whenever the call card opens because the dialer's own
 * in-call screen may change mute or speaker routing while our overlay is not visible.
 */
object CallAudioBus {
    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _speakerOn = MutableStateFlow(false)
    val speakerOn: StateFlow<Boolean> = _speakerOn.asStateFlow()

    fun refresh(context: Context) {
        val audio = context.getSystemService<AudioManager>() ?: return
        _muted.value = runCatching { audio.isMicrophoneMute }.getOrDefault(false)
        _speakerOn.value = runCatching { audio.isSpeakerOnCompat() }.getOrDefault(false)
    }

    fun toggleMute(context: Context) {
        val audio = context.getSystemService<AudioManager>() ?: return
        runCatching { audio.isMicrophoneMute = !audio.isMicrophoneMute }
        _muted.value = runCatching { audio.isMicrophoneMute }.getOrDefault(_muted.value)
    }

    fun toggleSpeaker(context: Context) {
        val audio = context.getSystemService<AudioManager>() ?: return
        runCatching { audio.setSpeakerOnCompat(!audio.isSpeakerOnCompat()) }
        _speakerOn.value = runCatching { audio.isSpeakerOnCompat() }.getOrDefault(_speakerOn.value)
    }

    private fun AudioManager.isSpeakerOnCompat(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            isSpeakerphoneOn
        }

    private fun AudioManager.setSpeakerOnCompat(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (enabled) {
                availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    ?.let(::setCommunicationDevice)
            } else {
                clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            isSpeakerphoneOn = enabled
        }
    }
}
