package com.vibecoding.millionaire.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File

/**
 * Plays the WAV files that the shared `FeedbackManager` read from the Compose resources. The bytes
 * are cached on disk once because `SoundPool` only knows how to load from a path or a file
 * descriptor, not from a byte array.
 */
class AndroidFeedback(context: Context) : PlatformFeedback {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val soundsDir = File(appContext.cacheDir, SOUNDS_DIR).apply { mkdirs() }
    private val soundIds = HashMap<String, Int>()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val vibrator: Vibrator? = runCatching {
        val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    }.getOrNull()

    override fun loadSound(name: String, bytes: ByteArray) {
        runCatching {
            val file = File(soundsDir, "$name.wav")
            file.writeBytes(bytes)
            soundIds[name] = soundPool.load(file.absolutePath, 1)
        }
    }

    override fun playSound(name: String) {
        val soundId = soundIds[name] ?: return
        handler.post { soundPool.play(soundId, 1f, 1f, 1, 0, 1f) }
    }

    override fun vibrate(durationMillis: Long) {
        val vibrator = vibrator ?: return
        val effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
        runCatching { vibrator.vibrate(effect) }
    }

    private companion object {
        const val SOUNDS_DIR = "sounds"
    }
}
