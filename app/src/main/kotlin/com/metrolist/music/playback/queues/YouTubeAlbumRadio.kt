/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.QueueData
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeAlbumRadio(
    private var playlistId: String,
    private var albumSongCount: Int = 0,
    private var continuation: String? = null,
    private var firstTimeLoaded: Boolean = false,
    private var restoredStatus: Queue.Status? = null,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private val endpoint: WatchEndpoint
        get() = WatchEndpoint(
            playlistId = playlistId
        )

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        restoredStatus?.let { status ->
            restoredStatus = null
            return@withContext status
        }

        val albumSongs = YouTube.albumSongs(playlistId).getOrThrow()
        albumSongCount = albumSongs.size
        Queue.Status(
            title = albumSongs.first().album?.name.orEmpty(),
            items = albumSongs.map { it.toMediaItem() },
            mediaItemIndex = 0
        )
    }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
        continuation = nextResult.continuation
        if (!firstTimeLoaded) {
            firstTimeLoaded = true
            nextResult.items.drop(albumSongCount).map { it.toMediaItem() }
        } else {
            nextResult.items.map { it.toMediaItem() }
        }
    }

    fun persistenceData() =
        QueueData.YouTubeAlbumRadioData(
            playlistId = playlistId,
            albumSongCount = albumSongCount,
            continuation = continuation,
            firstTimeLoaded = firstTimeLoaded,
        )

    companion object {
        fun restore(
            data: QueueData.YouTubeAlbumRadioData,
            status: Queue.Status,
        ): YouTubeAlbumRadio =
            YouTubeAlbumRadio(
                playlistId = data.playlistId,
                albumSongCount = data.albumSongCount,
                continuation = data.continuation,
                firstTimeLoaded = data.firstTimeLoaded,
                restoredStatus = status,
            )
    }
}
