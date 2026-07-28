/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.db.entities.PlaylistEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.util.UUID

@Serializable
internal enum class PendingPlaylistEditType {
    CREATE_PLAYLIST,
    ADD_SONG,
}

@Serializable
internal data class PendingPlaylistEdit(
    val id: String = UUID.randomUUID().toString(),
    val type: PendingPlaylistEditType,
    val playlistId: String,
    val playlistName: String? = null,
    val browseId: String? = null,
    val songId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

internal object PendingPlaylistEditCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun encode(edits: List<PendingPlaylistEdit>): String = json.encodeToString(edits)

    fun decode(value: String?): List<PendingPlaylistEdit> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<PendingPlaylistEdit>>(value)
        }.getOrDefault(emptyList())
    }
}

internal fun shouldUnbookmarkMissingPlaylist(
    playlist: PlaylistEntity,
    remoteIds: Set<String>,
    now: LocalDateTime,
    gracePeriodMinutes: Long,
): Boolean {
    val browseId = playlist.browseId ?: return false
    if (browseId in remoteIds || playlist.bookmarkedAt == null) return false

    val createdAt = playlist.createdAt ?: return true
    return !createdAt.isAfter(now.minusMinutes(gracePeriodMinutes))
}
