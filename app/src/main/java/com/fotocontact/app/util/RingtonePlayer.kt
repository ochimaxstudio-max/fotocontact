package com.fotocontact.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.fotocontact.app.data.FeatureCfg
import com.fotocontact.app.data.Prefs

object RingtonePlayer {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var savedRingVolume = -1
    private var savedNotifVolume = -1

    @Synchronized
    fun play(ctx: Context, cfg: FeatureCfg, asRingtone: Boolean) {
        stop(ctx)
        val app = ctx.applicationContext

        if (Prefs.silenceSystem(app)) silenceSystem(app)

        val toneUri = cfg.tone
        if (!toneUri.isNullOrBlank()) {
            try {
                val mp = MediaPlayer()
                mp.setDataSource(app, Uri.parse(toneUri))
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(
                            if (asRingtone) AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                            else AudioAttributes.USAGE_NOTIFICATION
                        )
                        .build()
                )
                mp.isLooping = asRingtone
                mp.prepare()
                mp.start()
                player = mp
            } catch (e: Exception) {
                player = null
            }
        }

        if (cfg.vibrate) {
            try {
                val v = vibratorOf(app)
                vibrator = v
                if (asRingtone) {
                    val pattern = longArrayOf(0, 700, 900)
                    v?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    v?.vibrate(VibrationEffect.createOneShot(220, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } catch (e: Exception) {
                // abaikan
            }
        }
    }

    @Synchronized
    fun stop(ctx: Context) {
        try {
            player?.stop()
            player?.release()
        } catch (e: Exception) {
            // abaikan
        }
        player = null
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            // abaikan
        }
        vibrator = null
        restoreSystem(ctx.applicationContext)
    }

    private fun vibratorOf(ctx: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun silenceSystem(ctx: Context) {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (savedRingVolume < 0) {
                savedRingVolume = am.getStreamVolume(AudioManager.STREAM_RING)
                am.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            }
            if (savedNotifVolume < 0) {
                savedNotifVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            }
        } catch (e: Exception) {
            // butuh akses Do Not Disturb pada sebagian perangkat
        }
    }

    private fun restoreSystem(ctx: Context) {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (savedRingVolume >= 0) {
                am.setStreamVolume(AudioManager.STREAM_RING, savedRingVolume, 0)
                savedRingVolume = -1
            }
            if (savedNotifVolume >= 0) {
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotifVolume, 0)
                savedNotifVolume = -1
            }
        } catch (e: Exception) {
            savedRingVolume = -1
            savedNotifVolume = -1
        }
    }
}
