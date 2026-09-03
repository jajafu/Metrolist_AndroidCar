package com.metrolist.music.playback

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongAlbumMap
import com.metrolist.music.db.entities.SongArtistMap
import com.metrolist.music.db.entities.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AndroidAutoLibraryTest {
    private lateinit var database: InternalDatabase
    private lateinit var dao: DatabaseDao
    private lateinit var library: AndroidAutoLibrary
    private val relationBatchSizes = mutableListOf<Int>()
    private val now = LocalDateTime.of(2026, 9, 1, 0, 0)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), InternalDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.dao
        val recordingDao = object : DatabaseDao by dao {
            override suspend fun getSongsByIds(songIds: List<String>): List<Song> {
                relationBatchSizes.add(songIds.size)
                return dao.getSongsByIds(songIds)
            }
        }
        library = AndroidAutoLibrary(recordingDao) { listOf("song-2", "song-0") }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `large library browse hydrates only the requested page`() = runBlocking {
        insertSongs(1_205)
        val ids = library.songIds(MusicService.SONG, 100, 1_000)
        assertEquals((1_000 until 1_100).map { "song-$it" }, library.songs(ids).map { it.id })
        assertEquals(listOf(100), relationBatchSizes)
        assertTrue(library.songIds(MusicService.SONG, 100, Long.MAX_VALUE).isEmpty())
    }

    @Test
    fun `full playlist queue preserves duplicates and order across relation batches`() = runBlocking {
        insertSongs(1_005)
        val ids = (0 until 1_005).map { "song-$it" } + listOf("song-2", "song-2", "song-0")
        dao.insert(PlaylistEntity(id = "playlist", name = "Playlist"))
        database.runInTransaction {
            ids.forEachIndexed { index, id ->
                dao.insert(PlaylistSongMap(playlistId = "playlist", songId = id, position = index))
            }
        }
        val selectedIds = library.songIds("${MusicService.PLAYLIST}/playlist")
        assertEquals(ids, selectedIds)
        assertEquals(ids, library.queueItems(selectedIds).map { it.mediaId })
        assertEquals(11, relationBatchSizes.size)
        assertTrue(relationBatchSizes.all { it <= CAR_PAGE_SIZE })
    }

    @Test
    fun `deleted songs do not prevent the remaining queue from loading`() = runBlocking {
        insertSongs(3)
        assertEquals(
            listOf("song-2", "song-0", "song-2"),
            library.queueItems(listOf("song-2", "missing", "song-0", "song-2")).map { it.mediaId },
        )
    }

    @Test
    fun `SQL search matches titles artists albums and playlists without duplicate results`() = runBlocking {
        insertSongs(5)
        dao.update(SongEntity(id = "song-0", title = "Needle song"))
        dao.insert(ArtistEntity(id = "artist", name = "Needle artist"))
        dao.insert(SongArtistMap(songId = "song-1", artistId = "artist", position = 0))
        dao.insert(AlbumEntity(id = "album", title = "Needle album", songCount = 1, duration = 1))
        dao.insert(SongAlbumMap(songId = "song-2", albumId = "album", index = 0))
        dao.insert(PlaylistEntity(id = "playlist", name = "Needle playlist"))
        listOf("song-1", "song-3", "song-3").forEachIndexed { index, id ->
            dao.insert(PlaylistSongMap(playlistId = "playlist", songId = id, position = index))
        }
        assertEquals((0..3).map { "song-$it" }, library.search("nEeDlE").map { it.id })
        assertEquals(listOf("song-2", "song-3"), library.search("needle", 2, 2).map { it.id })
    }

    @Test
    fun `broad search bounds relation loading and treats SQL metacharacters literally`() = runBlocking {
        insertSongs(1_005)
        assertEquals(100, library.search("Song").size)
        assertEquals(listOf(100), relationBatchSizes)
        dao.insert(SongEntity(id = "literal", title = "100%_\\' special"))
        assertEquals(listOf("literal"), library.search("%_\\'").map { it.id })
        assertTrue(library.search("' OR 1=1 --").isEmpty())
    }

    @Test
    fun `album artist liked and completed download selections retain their order`() = runBlocking {
        insertSongs(3)
        dao.insert(ArtistEntity(id = "artist", name = "Artist"))
        dao.insert(AlbumEntity(id = "album", title = "Album", songCount = 3, duration = 3))
        for (index in 0..2) {
            dao.insert(SongArtistMap(songId = "song-$index", artistId = "artist", position = 0))
            dao.insert(SongAlbumMap(songId = "song-$index", albumId = "album", index = 2 - index))
        }
        assertEquals(listOf("song-2", "song-1", "song-0"), library.songIds("${MusicService.ALBUM}/album"))
        assertEquals(listOf("song-0", "song-1", "song-2"), library.songIds("${MusicService.ARTIST}/artist"))
        assertEquals(listOf("song-2", "song-1", "song-0"), library.songIds("${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}"))
        assertEquals(listOf("song-0"), library.songIds("${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}", 1, 1))
    }

    @Test
    fun `folder queries apply limits before loading relationships`() = runBlocking {
        insertSongs(3)
        for (index in 0..2) {
            dao.insert(ArtistEntity(id = "artist-$index", name = "Artist $index"))
            dao.insert(SongArtistMap(songId = "song-$index", artistId = "artist-$index", position = 0))
            dao.insert(AlbumEntity(id = "album-$index", title = "Album", songCount = 1, duration = 1))
            dao.update(SongEntity(id = "song-$index", title = "Song", inLibrary = now, albumId = "album-$index"))
            dao.insert(PlaylistEntity(id = "playlist-$index", name = "Playlist", bookmarkedAt = now))
        }
        assertEquals(listOf("artist-1"), dao.androidAutoArtists(1, 1).map { it.id })
        assertEquals(listOf("album-1"), dao.androidAutoAlbums(1, 1).map { it.id })
        assertEquals(listOf("playlist-1"), dao.androidAutoPlaylists(1, 1).map { it.id })
    }

    @Test
    fun `shuffle and generated playlist headers occupy positions only on the first page`() = runBlocking {
        val rows = (0..10).map(Int::toString)
        suspend fun page(offset: Long, limit: Int) = loadCarPage(listOf("shuffle"), offset, limit) { count, start ->
            rows.slicePage(start, count)
        }
        assertEquals(listOf("shuffle", "0", "1"), page(0, 3))
        assertEquals(listOf("2", "3", "4"), page(3, 3))
        assertEquals(listOf("shuffle"), page(0, 1))
        assertEquals(listOf("0"), page(1, 1))
    }

    @Test
    fun `unpaged browsers get traversable bounded pages and invalid offsets are rejected`() {
        val first = CarPage.from("playlist/favorites", 0, Int.MAX_VALUE)!!
        assertEquals(100, first.size)
        assertTrue(first.showMore)
        val next = CarPage.from(first.nextId(99), 0, Int.MAX_VALUE)!!
        assertEquals(first.parentId, next.parentId)
        assertEquals(99L, next.offset)
        val nativePage = CarPage.from("song", 2, 25)!!
        assertEquals(50L, nativePage.offset)
        assertFalse(nativePage.showMore)
        assertNull(CarPage.from("song", -1, 100))
        assertNull(CarPage.from("song", 0, 0))
        assertNull(CarPage.from("android_auto_page/${Long.MAX_VALUE}/song", 1, 100))
        assertNull(CarPage.from("android_auto_page/bad/song", 0, 100))
    }

    @Test
    fun `browse artwork is a URI with no embedded image payload`() {
        val song = Song(
            SongEntity(id = "song", title = "Song", thumbnailUrl = "https://example.com/cover.jpg"),
            listOf(ArtistEntity(id = "artist", name = "Artist")),
        )
        val item = song.toCarMediaItem("playlist/favorites", " & ")
        assertEquals("playlist/favorites/song", item.mediaId)
        assertEquals("https://example.com/cover.jpg", item.mediaMetadata.artworkUri.toString())
        assertNull(item.mediaMetadata.artworkData)
        assertTrue(item.mediaMetadata.isPlayable == true)
        assertEquals("Artist", item.mediaMetadata.artist)
    }

    private fun insertSongs(count: Int) {
        database.runInTransaction {
            repeat(count) { index ->
                dao.insert(SongEntity(
                    id = "song-$index", title = "Song $index", inLibrary = now,
                    liked = true, likedDate = now.plusSeconds(index.toLong()),
                ))
            }
        }
    }
}
