/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.extensions

import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.PersistQueue
import com.metrolist.music.models.QueueData
import com.metrolist.music.models.QueueType
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.playback.queues.YouTubeAlbumRadio
import com.metrolist.music.playback.queues.YouTubeQueue

fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long
): PersistQueue {
    return when (this) {
        is ListQueue -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
        is YouTubeQueue -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE,
                queueData = persistenceData(),
            )
        }
        is YouTubeAlbumRadio -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE_ALBUM_RADIO,
                queueData = persistenceData(),
            )
        }
        is LocalAlbumRadio -> {
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.LIST,
            )
        }
        else -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
    }
}

fun PersistQueue.toQueue(): Queue {
    val restoredStatus =
        Queue.Status(
            title = title,
            items = items.map { it.toMediaItem() },
            mediaItemIndex = mediaItemIndex,
            position = position,
        )

    return when (queueType) {
        is QueueType.LIST -> ListQueue(
            title = title,
            items = restoredStatus.items,
            startIndex = mediaItemIndex,
            position = position
        )
        is QueueType.YOUTUBE -> {
            val data = queueData.restorableYouTubeDataOrNull()
            if (data != null) {
                YouTubeQueue.restore(data, restoredStatus)
            } else {
                restoredStatus.toListQueue()
            }
        }
        is QueueType.YOUTUBE_ALBUM_RADIO -> {
            val data = queueData as? QueueData.YouTubeAlbumRadioData
            if (data != null && data.playlistId != "youtube_album_radio") {
                YouTubeAlbumRadio.restore(data, restoredStatus)
            } else {
                restoredStatus.toListQueue()
            }
        }
        is QueueType.LOCAL_ALBUM_RADIO -> {
            restoredStatus.toListQueue()
        }
    }
}

private fun Queue.Status.toListQueue() =
    ListQueue(
        title = title,
        items = items,
        startIndex = mediaItemIndex,
        position = position,
    )

internal fun QueueData?.restorableYouTubeDataOrNull(): QueueData.YouTubeDataV2? =
    (this as? QueueData.YouTubeDataV2)
        ?.takeIf { it.videoId != null || it.playlistId != null }
