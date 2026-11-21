package com.vibecoding.millionaire.platform

/**
 * Platform side of sound (FR-8.1) and vibration (FR-8.2). The shared `FeedbackManager` reads the
 * WAV bytes from the Compose resources once and hands them over here; the platform only knows how
 * to play a loaded sound and how to buzz.
 */
interface PlatformFeedback {
    fun loadSound(name: String, bytes: ByteArray)

    fun playSound(name: String)

    fun vibrate(durationMillis: Long)
}
