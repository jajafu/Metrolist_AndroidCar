package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingSongLikesTest {
    @Test
    fun `rapid opposite updates keep only the latest desired state`() {
        val liked = pending(songId = "song", liked = true, sequence = 1)
        val unliked = pending(songId = "song", liked = false, sequence = 2)

        val result =
            coalescePendingSongLike(
                pending = listOf(liked),
                update = unliked,
            )

        assertEquals(listOf(unliked), result)
    }

    @Test
    fun `late coroutine cannot overwrite a newer update`() {
        val newer = pending(songId = "song", liked = false, sequence = 2)
        val older = pending(songId = "song", liked = true, sequence = 1)

        val result =
            coalescePendingSongLike(
                pending = listOf(newer),
                update = older,
            )

        assertEquals(listOf(newer), result)
    }

    @Test
    fun `completed update does not remove a newer pending state`() {
        val completed = pending(songId = "song", liked = true, sequence = 1)
        val newer = pending(songId = "song", liked = false, sequence = 2)

        val result =
            removeCompletedPendingSongLike(
                pending = listOf(newer),
                completed = completed,
            )

        assertEquals(listOf(newer), result)
    }

    @Test
    fun `pending likes codec round trips durable state`() {
        val updates =
            listOf(
                pending(songId = "first", liked = true, sequence = 1),
                pending(songId = "second", liked = false, sequence = 2),
            )

        val restored = PendingSongLikeCodec.decode(PendingSongLikeCodec.encode(updates))

        assertEquals(updates, restored)
        assertTrue(PendingSongLikeCodec.decode(null).isEmpty())
    }

    private fun pending(
        songId: String,
        liked: Boolean,
        sequence: Long,
    ) = PendingSongLike(
        songId = songId,
        title = songId,
        liked = liked,
        sequence = sequence,
    )
}
