package com.metrolist.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceSkipTargetTest {
    private val expected = SilenceSkipTarget(mediaId = "song-1", mediaItemIndex = 4)

    @Test
    fun `matching track remains eligible for silence skip`() {
        assertTrue(
            isCurrentSilenceSkipTarget(
                expected = expected,
                currentMediaId = "song-1",
                currentMediaItemIndex = 4,
            )
        )
    }

    @Test
    fun `media transition invalidates delayed silence skip`() {
        assertFalse(
            isCurrentSilenceSkipTarget(
                expected = expected,
                currentMediaId = "song-2",
                currentMediaItemIndex = 5,
            )
        )
        assertFalse(
            isCurrentSilenceSkipTarget(
                expected = expected,
                currentMediaId = "song-1",
                currentMediaItemIndex = 5,
            )
        )
    }
}
