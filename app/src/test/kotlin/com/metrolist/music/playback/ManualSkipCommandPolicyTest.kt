package com.metrolist.music.playback

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualSkipCommandPolicyTest {
    @Test
    fun `next and previous commands resume playback`() {
        assertTrue(isManualSkipCommand(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(isManualSkipCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertTrue(isManualSkipCommand(Player.COMMAND_SEEK_TO_PREVIOUS))
        assertTrue(isManualSkipCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
    }

    @Test
    fun `ordinary seek does not resume playback`() {
        assertFalse(isManualSkipCommand(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
    }
}
