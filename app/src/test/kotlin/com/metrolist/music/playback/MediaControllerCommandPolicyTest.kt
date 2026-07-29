package com.metrolist.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaControllerCommandPolicyTest {
    @Test
    fun `trusted controller can use custom commands`() {
        assertTrue(MediaControllerCommandPolicy.canUseCustomCommands(isTrustedController = true))
    }

    @Test
    fun `untrusted controller cannot use custom commands`() {
        assertFalse(MediaControllerCommandPolicy.canUseCustomCommands(isTrustedController = false))
    }
}
