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
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class PlaylistDuplicateQueriesTest {
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
    fun `large playlist query returns only downloaded song IDs in playlist order`() = runBlocking {
        dao.insert(PlaylistEntity(id = "playlist", name = "Large playlist"))
        database.runInTransaction {
            (0 until 1_005).forEach { index ->
                val downloaded = index == 7 || index == 1_002
                val id = "song-$index"
                dao.insert(
                    SongEntity(
                        id = id,
                        title = id,
                        dateDownload = if (downloaded) LocalDateTime.now() else null,
                    )
                )
                dao.insert(
                    PlaylistSongMap(
                        playlistId = "playlist",
                        songId = id,
                        position = index,
                    )
                )
            }
        }

        assertEquals(
            listOf("song-7", "song-1002"),
            dao.downloadedPlaylistSongIds("playlist"),
        )
        assertEquals(1_004, dao.maxPlaylistSongPosition("playlist"))
    }
}
