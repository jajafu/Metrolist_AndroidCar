package com.metrolist.music.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamClientSelectionPolicyTest {
    @Test
    fun `normal music keeps WEB_REMIX fast path`() {
        assertTrue(
            shouldSkipWebRemixValidation(
                clientName = "WEB_REMIX",
                webRemixPreviouslyFailed = false,
                musicVideoType = "MUSIC_VIDEO_TYPE_ATV",
            )
        )
    }

    @Test
    fun `UGC podcast and unknown media require validation`() {
        listOf("MUSIC_VIDEO_TYPE_UGC", "MUSIC_VIDEO_TYPE_PODCAST_EPISODE", null, "").forEach { type ->
            assertFalse(
                shouldSkipWebRemixValidation(
                    clientName = "WEB_REMIX",
                    webRemixPreviouslyFailed = false,
                    musicVideoType = type,
                )
            )
        }
    }

    @Test
    fun `failed WEB_REMIX client is excluded on the next resolution`() {
        assertTrue(shouldSkipFailedWebRemixClient("WEB_REMIX", webRemixPreviouslyFailed = true))
        assertFalse(shouldSkipFailedWebRemixClient("TVHTML5", webRemixPreviouslyFailed = true))
    }
}
