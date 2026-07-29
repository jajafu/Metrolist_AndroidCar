package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `offline like wins over a stale remote unlike`() {
        val result =
            resolveLikedStateDuringRemoteReconciliation(
                localLiked = true,
                isLocalSong = false,
                remoteLiked = false,
                pending = pending(songId = "song", liked = true, sequence = 1),
            )

        assertTrue(result)
    }

    @Test
    fun `failed pending unlike is not reversed by stale remote like`() {
        val result =
            resolveLikedStateDuringRemoteReconciliation(
                localLiked = false,
                isLocalSong = false,
                remoteLiked = true,
                pending = pending(songId = "song", liked = false, sequence = 2),
            )

        assertFalse(result)
    }

    @Test
    fun `remote unlike wins when there is no pending local action`() {
        val result =
            resolveLikedStateDuringRemoteReconciliation(
                localLiked = true,
                isLocalSong = false,
                remoteLiked = false,
                pending = null,
            )

        assertFalse(result)
    }

    @Test
    fun `local files keep their local liked state`() {
        val result =
            resolveLikedStateDuringRemoteReconciliation(
                localLiked = true,
                isLocalSong = true,
                remoteLiked = false,
                pending = null,
            )

        assertTrue(result)
    }

    @Test
    fun `latest sequence controls reconciliation after rapid opposite actions`() {
        val latest =
            latestPendingSongLikesById(
                listOf(
                    pending(songId = "song", liked = true, sequence = 1),
                    pending(songId = "song", liked = false, sequence = 2),
                ),
            ).getValue("song")

        assertFalse(
            resolveLikedStateDuringRemoteReconciliation(
                localLiked = true,
                isLocalSong = false,
                remoteLiked = true,
                pending = latest,
            ),
        )
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
