package com.vikram.expressiveisland.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Plays the sound and vibration a notification would have made.
 */
class NotificationAlerter(private val context: Context) {

    private val audioManager by lazy {
        context.getSystemService(AudioManager::class.java)
    }

    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    private var playing: Ringtone? = null

    fun alert(
        channel: NotificationChannel?,
        importance: Int,
    ) {
        if (importance < NotificationManager.IMPORTANCE_DEFAULT) return

        val ringerMode = audioManager?.ringerMode ?: return

        if (ringerMode == AudioManager.RINGER_MODE_SILENT) return

        val wantsSound = channel == null || channel.sound != null
        val wantsBuzz = channel == null || channel.shouldVibrate()

        if (!wantsSound && !wantsBuzz) return

        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            buzz(channel)
            return
        }

        if (wantsSound) {
            play(
                channel?.sound
                    ?: RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION,
                    ),
                channel?.audioAttributes,
            )
        }

        if (wantsBuzz) {
            buzz(channel)
        }
    }

    @Synchronized
    fun stop() {
        runCatching {
            playing?.takeIf { it.isPlaying }?.stop()
        }.onFailure {
            Log.w(TAG, "Failed to stop notification sound", it)
        }

        playing = null
    }

    @Synchronized
    private fun play(
        uri: Uri?,
        attributes: AudioAttributes?,
    ) {
        val sound = uri ?: return

        stop()

        runCatching {
            val ringtone = RingtoneManager.getRingtone(
                context,
                sound,
            ) ?: return

            ringtone.audioAttributes =
                attributes ?: NOTIFICATION_ATTRIBUTES

            ringtone.play()

            playing = ringtone
        }.onFailure {
            Log.w(TAG, "Failed to play notification sound", it)
        }
    }

    private fun buzz(channel: NotificationChannel?) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() }
            ?: return

        val pattern = channel?.vibrationPattern

        val effect =
            if (pattern != null && pattern.isNotEmpty()) {
                VibrationEffect.createWaveform(
                    pattern,
                    NO_REPEAT,
                )
            } else {
                VibrationEffect.createWaveform(
                    DEFAULT_PATTERN,
                    DEFAULT_AMPLITUDES,
                    NO_REPEAT,
                )
            }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(
                        VibrationAttributes.USAGE_NOTIFICATION,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    NOTIFICATION_ATTRIBUTES,
                )
            }
        }.onFailure {
            Log.w(TAG, "Failed to vibrate", it)
        }
    }

    private companion object {
        const val TAG = "NotificationAlerter"

        const val NO_REPEAT = -1

        val DEFAULT_PATTERN =
            longArrayOf(0, 250, 250, 250)

        val DEFAULT_AMPLITUDES =
            intArrayOf(0, 255, 0, 255)

        val NOTIFICATION_ATTRIBUTES =
            AudioAttributes.Builder()
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION,
                )
                .setUsage(
                    AudioAttributes.USAGE_NOTIFICATION,
                )
                .build()
    }
}