package com.metrolist.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAudioFocusStateTest {
    @Test
    fun `duck keeps playback allowed and gain restores full volume`() {
        val state = PlaybackAudioFocusState()
        state.onFocusRequestGranted()

        state.onDuck()

        assertTrue(state.canPlayAudio)
        assertEquals(0.2f, state.volumeMultiplier)
        assertFalse(state.onFocusGain())
        assertTrue(state.canPlayAudio)
        assertEquals(1f, state.volumeMultiplier)
    }

    @Test
    fun `direct request grant clears a stuck duck multiplier`() {
        val state = PlaybackAudioFocusState()
        state.onDuck()

        state.onFocusRequestGranted()

        assertTrue(state.canPlayAudio)
        assertEquals(1f, state.volumeMultiplier)
    }

    @Test
    fun `direct request grant cancels a pending automatic resume`() {
        val state = PlaybackAudioFocusState()
        state.onTransientLoss(wasPlaying = true)

        state.onFocusRequestGranted()

        assertFalse(state.onFocusGain())
    }

    @Test
    fun `transient loss resumes only after focus gain`() {
        val state = PlaybackAudioFocusState()
        state.onFocusRequestGranted()

        state.onTransientLoss(wasPlaying = true)

        assertFalse(state.canPlayAudio)
        assertTrue(state.onFocusGain())
        assertFalse(state.onFocusGain())
    }

    @Test
    fun `repeated transient loss preserves pending resume`() {
        val state = PlaybackAudioFocusState()

        state.onTransientLoss(wasPlaying = true)
        state.onTransientLoss(wasPlaying = false)

        assertTrue(state.onFocusGain())
    }

    @Test
    fun `permanent loss cancels pending resume`() {
        val state = PlaybackAudioFocusState()
        state.onTransientLoss(wasPlaying = true)

        state.onPermanentLoss()

        assertFalse(state.canPlayAudio)
        assertEquals(1f, state.volumeMultiplier)
        assertFalse(state.onFocusGain())
    }
}
