package com.metrolist.music.db

import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist

interface AndroidAutoDao {
    @RawQuery
    suspend fun androidAutoSongIds(query: SupportSQLiteQuery): List<String>

    @Query("SELECT browseId FROM playlist WHERE bookmarkedAt IS NOT NULL AND browseId IS NOT NULL")
    suspend fun androidAutoSavedBrowseIds(): List<String>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id
        WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist WHERE songCount > 0 ORDER BY rowId LIMIT :limit OFFSET :offset
    """)
    suspend fun androidAutoArtists(limit: Int, offset: Long): List<Artist>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("""
        SELECT * FROM album WHERE EXISTS(SELECT 1 FROM song
        WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL)
        ORDER BY rowId LIMIT :limit OFFSET :offset
    """)
    suspend fun androidAutoAlbums(limit: Int, offset: Long): List<Album>

    @Transaction
    @Query("""
        SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY rowId LIMIT :limit OFFSET :offset
    """)
    suspend fun androidAutoPlaylists(limit: Int, offset: Long): List<Playlist>
}
