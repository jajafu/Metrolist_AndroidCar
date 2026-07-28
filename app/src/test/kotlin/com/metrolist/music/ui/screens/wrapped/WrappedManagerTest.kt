package com.metrolist.music.ui.screens.wrapped

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WrappedManagerTest {
    @Test
    fun `zero songs returns no track selection`() {
        assertNull(selectWrappedTracks(emptyList()))
    }

    @Test
    fun `one song is reused for every wrapped section`() {
        val selection = selectWrappedTracks(songIds(1))

        assertEquals(
            WrappedTrackSelection("song-0", "song-0", "song-0"),
            selection
        )
    }

    @Test
    fun `two songs use last song when later ranges are unavailable`() {
        val selection = selectWrappedTracks(songIds(2))

        assertEquals(
            WrappedTrackSelection("song-1", "song-0", "song-1"),
            selection
        )
    }

    @Test
    fun `four songs select end song only from available third through fifth range`() {
        val selection = requireNotNull(selectWrappedTracks(songIds(4)))

        assertEquals("song-3", selection.introSongId)
        assertEquals("song-0", selection.topSongId)
        assertTrue(selection.endSongId in setOf("song-2", "song-3"))
    }

    @Test
    fun `five songs keep bounded end range and safe intro fallback`() {
        val selection = requireNotNull(selectWrappedTracks(songIds(5)))

        assertEquals("song-4", selection.introSongId)
        assertEquals("song-0", selection.topSongId)
        assertTrue(selection.endSongId in setOf("song-2", "song-3", "song-4"))
    }

    @Test
    fun `more than five songs preserve intro and end selection ranges`() {
        val selection = requireNotNull(selectWrappedTracks(songIds(8)))

        assertTrue(selection.introSongId in setOf("song-5", "song-6", "song-7"))
        assertEquals("song-0", selection.topSongId)
        assertTrue(selection.endSongId in setOf("song-2", "song-3", "song-4"))
    }

    private fun songIds(size: Int) = List(size) { "song-$it" }
}
