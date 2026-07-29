package com.metrolist.music.ui.screens

import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import org.junit.Assert.assertSame
import org.junit.Test

class HomeSongStateTest {
    private val originalSong = Song(
        song = SongEntity(id = "song-id", title = "Original"),
        artists = emptyList(),
    )

    @Test
    fun `uses updated database song while it exists`() {
        val updatedSong = Song(
            song = SongEntity(id = "song-id", title = "Updated"),
            artists = emptyList(),
        )

        assertSame(updatedSong, resolveCurrentHomeSong(updatedSong, originalSong))
    }

    @Test
    fun `falls back when database song disappears`() {
        assertSame(originalSong, resolveCurrentHomeSong(null, originalSong))
    }
}
