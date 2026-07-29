package com.metrolist.music.utils

import com.metrolist.music.db.entities.PlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PlaylistDuplicateCleanupTest {
    private val now = LocalDateTime.of(2026, 7, 29, 12, 0)

    @Test
    fun `duplicate remote entries keep one item in stable order`() {
        val playlists = listOf("remote-2" to "first", "remote-1" to "only", "remote-2" to "second")

        assertEquals(
            listOf("remote-2" to "first", "remote-1" to "only"),
            deduplicateRemotePlaylists(playlists) { it.first },
        )
    }

    @Test
    fun `pending duplicate is retained and unprotected duplicate is deleted`() {
        val older = playlist(id = "local-old", remoteSongCount = 500, createdAt = now.minusDays(2))
        val pending = playlist(id = "local-pending", remoteSongCount = 1, createdAt = now.minusDays(1))

        val plan = planPlaylistDuplicateCleanup(
            playlists = listOf(older, pending),
            protectedPlaylistIds = setOf(pending.id),
        )

        assertEquals(pending, plan.keeper)
        assertEquals(listOf(older), plan.duplicatesToDelete)
    }

    @Test
    fun `multiple protected duplicates are never scheduled for deletion`() {
        val pending = playlist(id = "pending", remoteSongCount = 1, createdAt = now.minusDays(2))
        val recent = playlist(id = "recent", remoteSongCount = 2, createdAt = now.minusMinutes(5))
        val stale = playlist(id = "stale", remoteSongCount = 1_000, createdAt = now.minusDays(5))

        val plan = planPlaylistDuplicateCleanup(
            playlists = listOf(stale, recent, pending),
            protectedPlaylistIds = setOf(pending.id, recent.id),
        )

        assertTrue(plan.keeper.id in setOf(pending.id, recent.id))
        assertEquals(listOf(stale), plan.duplicatesToDelete)
    }

    @Test
    fun `recent remote playlist is inside reconciliation grace`() {
        assertTrue(
            isRecentlyCreatedRemotePlaylist(
                playlist = playlist(id = "recent", remoteSongCount = 0, createdAt = now.minusMinutes(5)),
                now = now,
                gracePeriodMinutes = 15,
            )
        )
    }

    private fun playlist(
        id: String,
        remoteSongCount: Int,
        createdAt: LocalDateTime,
    ) = PlaylistEntity(
        id = id,
        name = id,
        browseId = "remote",
        createdAt = createdAt,
        lastUpdateTime = createdAt,
        bookmarkedAt = createdAt,
        remoteSongCount = remoteSongCount,
    )
}
