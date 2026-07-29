package com.metrolist.music.db

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class DatabaseDaoChunkingTest {
    private lateinit var database: InternalDatabase
    private lateinit var dao: DatabaseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room
            .inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.dao
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `empty ID lists return empty results`() = runBlocking {
        assertEquals(emptyList<Any>(), dao.getSongsByIds(emptyList()))
        assertEquals(emptyList<String>(), dao.playlistDuplicates("playlist", emptyList()))
    }

    @Test
    fun `small song query preserves requested order`() = runBlocking {
        insertSongs(listOf("song-1", "song-2", "song-3"))

        val result = dao.getSongsByIds(listOf("song-3", "missing", "song-1"))

        assertEquals(listOf("song-3", "song-1"), result.map { it.id })
    }

    @Test
    fun `duplicate IDs produce each existing row once`() = runBlocking {
        insertSongs(listOf("song-1", "song-2"))
        dao.insert(PlaylistEntity(id = "playlist", name = "Playlist"))
        dao.insert(PlaylistSongMap(playlistId = "playlist", songId = "song-2"))

        assertEquals(
            listOf("song-2", "song-1"),
            dao.getSongsByIds(listOf("song-2", "song-2", "song-1", "song-1")).map { it.id },
        )
        assertEquals(
            listOf("song-2"),
            dao.playlistDuplicates("playlist", listOf("song-2", "song-2", "song-1")),
        )
    }

    @Test
    fun `multi chunk queries handle more than one thousand IDs`() = runBlocking {
        val songIds = (0 until 1_005).map { "song-$it" }
        insertSongs(songIds)
        dao.insert(PlaylistEntity(id = "playlist", name = "Playlist"))
        listOf(0, 899, 900, 1_004).forEach { index ->
            dao.insert(
                PlaylistSongMap(
                    playlistId = "playlist",
                    songId = songIds[index],
                    position = index,
                )
            )
        }

        assertEquals(songIds, dao.getSongsByIds(songIds).map { it.id })
        assertEquals(
            listOf("song-0", "song-899", "song-900", "song-1004"),
            dao.playlistDuplicates("playlist", songIds),
        )
    }

    private fun insertSongs(songIds: List<String>) {
        database.runInTransaction {
            songIds.forEach { id ->
                dao.insert(SongEntity(id = id, title = id))
            }
        }
    }
}
