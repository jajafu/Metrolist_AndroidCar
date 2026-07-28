/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

internal class PlaybackAudioFocusState {
    private enum class Mode {
        NONE,
        FULL,
        DUCKED,
        TRANSIENT_LOSS,
    }

    private var mode = Mode.NONE
    private var resumeOnFocusGain = false

    val canPlayAudio: Boolean
        get() = mode == Mode.FULL || mode == Mode.DUCKED

    val volumeMultiplier: Float
        get() = if (mode == Mode.DUCKED) DUCK_VOLUME_MULTIPLIER else 1f

    fun onFocusGain(): Boolean {
        val shouldResume = resumeOnFocusGain
        mode = Mode.FULL
        resumeOnFocusGain = false
        return shouldResume
    }

    fun onFocusRequestGranted() {
        mode = Mode.FULL
        resumeOnFocusGain = false
    }

    fun onDuck() {
        mode = Mode.DUCKED
    }

    fun onTransientLoss(wasPlaying: Boolean) {
        mode = Mode.TRANSIENT_LOSS
        resumeOnFocusGain = resumeOnFocusGain || wasPlaying
    }

    fun onPermanentLoss() {
        mode = Mode.NONE
        resumeOnFocusGain = false
    }

    fun onAbandoned() {
        mode = Mode.NONE
        resumeOnFocusGain = false
    }

    private companion object {
        const val DUCK_VOLUME_MULTIPLIER = 0.2f
    }
}
