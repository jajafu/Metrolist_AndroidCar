/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.db.entities.PlaylistEntity
import java.time.LocalDateTime

internal data class PlaylistDuplicateCleanupPlan(
    val keeper: PlaylistEntity,
    val duplicatesToDelete: List<PlaylistEntity>,
)

internal fun <T> deduplicateRemotePlaylists(
    playlists: List<T>,
    idSelector: (T) -> String,
): List<T> = playlists.distinctBy(idSelector)

internal fun isRecentlyCreatedRemotePlaylist(
    playlist: PlaylistEntity,
    now: LocalDateTime,
    gracePeriodMinutes: Long,
): Boolean {
    if (playlist.browseId == null) return false
    val createdAt = playlist.createdAt ?: return false
    return createdAt.isAfter(now.minusMinutes(gracePeriodMinutes))
}

internal fun planPlaylistDuplicateCleanup(
    playlists: List<PlaylistEntity>,
    protectedPlaylistIds: Set<String>,
): PlaylistDuplicateCleanupPlan {
    require(playlists.isNotEmpty())

    val ranked = playlists.sortedWith(
        compareByDescending<PlaylistEntity> { it.id in protectedPlaylistIds }
            .thenByDescending { it.bookmarkedAt != null }
            .thenByDescending { it.remoteSongCount ?: -1 }
            .thenByDescending { it.lastUpdateTime ?: LocalDateTime.MIN }
            .thenBy { it.createdAt ?: LocalDateTime.MAX }
            .thenBy { it.id }
    )
    val keeper = ranked.first()
    return PlaylistDuplicateCleanupPlan(
        keeper = keeper,
        duplicatesToDelete = ranked.drop(1).filterNot { it.id in protectedPlaylistIds },
    )
}
