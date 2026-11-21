package com.vibecoding.millionaire.ui.feedback

import com.vibecoding.millionaire.domain.repository.SettingsRepository
import com.vibecoding.millionaire.platform.PlatformFeedback
import com.vibecoding.millionaire.platform.logWarn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.vibecoding.millionaire.generated.resources.Res

enum class SoundEffect(val fileName: String) {
    CLICK("click"),
    SELECT("select"),
    CORRECT("correct"),
    WRONG("wrong"),
    WIN("win"),
    WALK_AWAY("walkaway"),
}

enum class HapticEffect { TICK, SUCCESS, ERROR }

/**
 * Single entry point for sound (FR-8.1) and vibration (FR-8.2). Both respect the settings stored
 * in DataStore, so nothing has to be checked by the callers. The WAV bytes live in the Compose
 * resources and are handed to the platform once, so playback itself never blocks.
 */
class FeedbackManager(
    private val platform: PlatformFeedback,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
) {
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
        scope.launch { preload() }
    }

    private suspend fun preload() {
        SoundEffect.entries.forEach { effect ->
            runCatching { Res.readBytes(SOUND_PATH + effect.fileName + ".wav") }
                .onSuccess { platform.loadSound(effect.fileName, it) }
                .onFailure { logWarn(TAG, "Cannot load the '${effect.fileName}' sound: ${it.message}") }
        }
    }

    fun play(effect: SoundEffect) {
        if (!soundEnabled) return
        platform.playSound(effect.fileName)
    }

    fun haptic(effect: HapticEffect) {
        if (!vibrationEnabled) return
        platform.vibrate(
            when (effect) {
                HapticEffect.TICK -> 20L
                HapticEffect.SUCCESS -> 70L
                HapticEffect.ERROR -> 220L
            },
        )
    }

    private companion object {
        const val TAG = "Feedback"
        const val SOUND_PATH = "files/sounds/"
    }
}
