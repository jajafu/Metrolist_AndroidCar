package com.metrolist.music.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetActionSecurityTest {
    @Test
    fun `playback-changing widget actions require authentication`() {
        assertTrue(
            WidgetActionSecurity.requiresAuthentication(MusicWidgetReceiver.ACTION_PLAY_PAUSE),
        )
        assertTrue(
            WidgetActionSecurity.requiresAuthentication(MusicWidgetReceiver.ACTION_LIKE),
        )
        assertTrue(
            WidgetActionSecurity.requiresAuthentication(MusicWidgetReceiver.ACTION_NEXT),
        )
        assertTrue(
            WidgetActionSecurity.requiresAuthentication(MusicWidgetReceiver.ACTION_PREVIOUS),
        )
        assertTrue(
            WidgetActionSecurity.requiresAuthentication(PlaylistWidgetReceiver.ACTION_PLAY_TARGET),
        )
    }

    @Test
    fun `non-mutating widget refresh does not require authentication`() {
        assertFalse(
            WidgetActionSecurity.requiresAuthentication(MusicWidgetReceiver.ACTION_UPDATE_WIDGET),
        )
        assertFalse(WidgetActionSecurity.requiresAuthentication(null))
    }
}
