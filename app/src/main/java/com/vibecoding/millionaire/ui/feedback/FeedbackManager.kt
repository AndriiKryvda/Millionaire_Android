package com.vibecoding.millionaire.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.vibecoding.millionaire.R
import com.vibecoding.millionaire.di.ApplicationScope
import com.vibecoding.millionaire.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEffect(val resId: Int) {
    CLICK(R.raw.click),
    SELECT(R.raw.select),
    CORRECT(R.raw.correct),
    WRONG(R.raw.wrong),
    WIN(R.raw.win),
    WALK_AWAY(R.raw.walkaway),
}

enum class HapticEffect { TICK, SUCCESS, ERROR }

/**
 * Single entry point for sound (FR-8.1) and vibration (FR-8.2). Both respect the settings stored
 * in DataStore, so nothing has to be checked by the callers.
 */
@Singleton
class FeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var soundPool: SoundPool? = null
    private val soundIds = HashMap<SoundEffect, Int>()
    private val vibrator: Vibrator? = runCatching {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    }.getOrNull()

    @Volatile
    private var soundEnabled = true

    @Volatile
    private var vibrationEnabled = true

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                soundEnabled = settings.soundEnabled
                vibrationEnabled = settings.vibrationEnabled
            }
        }
        handler.post {
            val pool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
            soundPool = pool
            SoundEffect.entries.forEach { effect -> soundIds[effect] = pool.load(context, effect.resId, 1) }
        }
    }

    fun play(effect: SoundEffect) {
        if (!soundEnabled) return
        val pool = soundPool ?: return
        val soundId = soundIds[effect] ?: return
        handler.post { pool.play(soundId, 1f, 1f, 1, 0, 1f) }
    }

    fun haptic(effect: HapticEffect) {
        if (!vibrationEnabled) return
        val vibrator = vibrator ?: return
        val vibration = when (effect) {
            HapticEffect.TICK -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
            HapticEffect.SUCCESS -> VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
            HapticEffect.ERROR -> VibrationEffect.createOneShot(220, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        runCatching { vibrator.vibrate(vibration) }
    }
}