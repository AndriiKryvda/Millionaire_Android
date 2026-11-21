package com.vibecoding.millionaire.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategorySoloAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyleHeavy
import platform.UIKit.UIImpactFeedbackStyleLight
import platform.UIKit.UIImpactFeedbackStyleMedium

/**
 * iOS sound and haptics. Sounds are decoded once from the Compose resources and kept in memory;
 * the session is configured as `soloAmbient` so game sounds mix with (and pause) the music the
 * player may be listening to.
 */
@OptIn(ExperimentalForeignApi::class)
class IosFeedback : PlatformFeedback {

    private val players = HashMap<String, AVAudioPlayer>()

    init {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategorySoloAmbient, error = null)
            session.setActive(true)
        }
    }

    override fun loadSound(name: String, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        runCatching {
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            val player = AVAudioPlayer.createWithData(data = data, error = null) ?: return
            player.prepareToPlay()
            players[name] = player
        }
    }

    override fun playSound(name: String) {
        players[name]?.play()
    }

    override fun vibrate(durationMillis: Long) {
        val style = when {
            durationMillis <= 30L -> UIImpactFeedbackStyleLight
            durationMillis <= 120L -> UIImpactFeedbackStyleMedium
            else -> UIImpactFeedbackStyleHeavy
        }
        runCatching {
            val generator = UIImpactFeedbackGenerator(feedbackStyle = style)
            generator.prepare()
            generator.impactOccurred()
        }
    }
}
