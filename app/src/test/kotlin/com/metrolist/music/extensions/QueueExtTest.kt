package com.metrolist.music.extensions

import androidx.media3.common.MediaItem
import com.metrolist.music.models.QueueData
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.playback.queues.YouTubeQueue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueExtTest {
    @Test
    fun `restores YouTube continuation with the saved queue`() = runBlocking {
        val data =
            QueueData.YouTubeDataV2(
                videoId = "song-id",
                playlistId = "RDAMVMsong-id",
                continuation = "continuation-token",
            )
        val queue =
            YouTubeQueue.restore(
                data = data,
                status =
                    Queue.Status(
                        title = "Radio",
                        items = listOf(MediaItem.Builder().setMediaId("song-id").build()),
                        mediaItemIndex = 0,
                        position = 42_000L,
                    ),
            )

        assertTrue(queue.hasNextPage())
        val status = queue.getInitialStatus()
        assertEquals("Radio", status.title)
        assertEquals("song-id", status.items.single().mediaId)
        assertEquals(42_000L, status.position)
        assertEquals(data, queue.persistenceData())
    }

    @Test
    fun `legacy placeholder is not treated as restorable state`() {
        val legacyData = QueueData.YouTubeData(endpoint = "youtube_queue")

        assertNull(legacyData.restorableYouTubeDataOrNull())
    }
}
