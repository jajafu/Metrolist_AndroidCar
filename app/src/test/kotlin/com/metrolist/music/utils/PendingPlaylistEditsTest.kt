package com.metrolist.music.utils

import com.metrolist.music.db.entities.PlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PendingPlaylistEditsTest {
    private val now = LocalDateTime.of(2026, 7, 28, 12, 0)

    @Test
    fun `pending edits survive serialization`() {
        val edits =
            listOf(
                PendingPlaylistEdit(
                    id = "create-1",
                    type = PendingPlaylistEditType.CREATE_PLAYLIST,
                    playlistId = "local-1",
                    playlistName = "Road trip",
                    browseId = "remote-1",
                    createdAtEpochMs = 123L,
                ),
                PendingPlaylistEdit(
                    id = "add-1",
                    type = PendingPlaylistEditType.ADD_SONG,
                    playlistId = "local-1",
                    browseId = "remote-1",
                    songId = "song-1",
                    createdAtEpochMs = 456L,
                ),
                PendingPlaylistEdit(
                    id = "remove-1",
                    type = PendingPlaylistEditType.REMOVE_SONG,
                    playlistId = "local-1",
                    browseId = "remote-1",
                    songId = "song-2",
                    setVideoId = "set-video-2",
                    createdAtEpochMs = 789L,
                ),
            )

        assertEquals(edits, PendingPlaylistEditCodec.decode(PendingPlaylistEditCodec.encode(edits)))
    }

    @Test
    fun `removing an unsynced addition cancels the pending add`() {
        val pendingAdd =
            PendingPlaylistEdit(
                id = "add-1",
                type = PendingPlaylistEditType.ADD_SONG,
                playlistId = "local-1",
                browseId = "remote-1",
                songId = "song-1",
            )
        val removal =
            PendingPlaylistEdit(
                id = "remove-1",
                type = PendingPlaylistEditType.REMOVE_SONG,
                playlistId = "local-1",
                browseId = "remote-1",
                songId = "song-1",
            )

        val plan = planPendingPlaylistRemoval(listOf(pendingAdd), removal)

        assertEquals(emptyList<PendingPlaylistEdit>(), plan.edits)
        assertFalse(plan.removalQueued)
    }

    @Test
    fun `removing a synced duplicate keeps additions and queues exact occurrence`() {
        val pendingAdd =
            PendingPlaylistEdit(
                id = "add-1",
                type = PendingPlaylistEditType.ADD_SONG,
                playlistId = "local-1",
                browseId = "remote-1",
                songId = "song-1",
            )
        val removal =
            PendingPlaylistEdit(
                id = "remove-1",
                type = PendingPlaylistEditType.REMOVE_SONG,
                playlistId = "local-1",
                browseId = "remote-1",
                songId = "song-1",
                setVideoId = "set-video-existing",
            )

        val plan = planPendingPlaylistRemoval(listOf(pendingAdd), removal)

        assertEquals(listOf(pendingAdd, removal), plan.edits)
        assertTrue(plan.removalQueued)
    }

    @Test
    fun `invalid pending edit data is ignored safely`() {
        assertEquals(emptyList<PendingPlaylistEdit>(), PendingPlaylistEditCodec.decode("not-json"))
    }

    @Test
    fun `new remote playlist is protected during reconciliation grace period`() {
        val playlist =
            remotePlaylist(
                createdAt = now.minusMinutes(5),
                bookmarkedAt = now.minusMinutes(5),
            )

        assertFalse(
            shouldUnbookmarkMissingPlaylist(
                playlist = playlist,
                remoteIds = emptySet(),
                now = now,
                gracePeriodMinutes = 15,
            ),
        )
    }

    @Test
    fun `older missing remote playlist is explicitly unbookmarked`() {
        val playlist =
            remotePlaylist(
                createdAt = now.minusMinutes(16),
                bookmarkedAt = now.minusMinutes(16),
            )

        assertTrue(
            shouldUnbookmarkMissingPlaylist(
                playlist = playlist,
                remoteIds = emptySet(),
                now = now,
                gracePeriodMinutes = 15,
            ),
        )
    }

    @Test
    fun `present or already unbookmarked playlist is not changed`() {
        val present = remotePlaylist(createdAt = now.minusDays(1), bookmarkedAt = now.minusDays(1))
        val alreadyUnbookmarked = present.copy(bookmarkedAt = null)

        assertFalse(
            shouldUnbookmarkMissingPlaylist(
                playlist = present,
                remoteIds = setOf("remote-1"),
                now = now,
                gracePeriodMinutes = 15,
            ),
        )
        assertFalse(
            shouldUnbookmarkMissingPlaylist(
                playlist = alreadyUnbookmarked,
                remoteIds = emptySet(),
                now = now,
                gracePeriodMinutes = 15,
            ),
        )
    }

    private fun remotePlaylist(
        createdAt: LocalDateTime,
        bookmarkedAt: LocalDateTime?,
    ) = PlaylistEntity(
        id = "local-1",
        name = "Road trip",
        browseId = "remote-1",
        createdAt = createdAt,
        bookmarkedAt = bookmarkedAt,
    )
}
