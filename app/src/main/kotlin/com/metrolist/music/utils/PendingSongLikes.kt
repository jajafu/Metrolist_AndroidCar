/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class PendingSongLike(
    val songId: String,
    val title: String,
    val liked: Boolean,
    val sequence: Long,
)

internal fun coalescePendingSongLike(
    pending: List<PendingSongLike>,
    update: PendingSongLike,
): List<PendingSongLike> {
    val existing = pending.firstOrNull { it.songId == update.songId }
    if (existing != null && existing.sequence > update.sequence) return pending

    return (pending.filterNot { it.songId == update.songId } + update)
        .sortedBy(PendingSongLike::sequence)
}

internal fun removeCompletedPendingSongLike(
    pending: List<PendingSongLike>,
    completed: PendingSongLike,
): List<PendingSongLike> =
    pending.filterNot {
        it.songId == completed.songId && it.sequence == completed.sequence
    }

internal fun latestPendingSongLikesById(
    pending: List<PendingSongLike>,
): Map<String, PendingSongLike> =
    pending
        .groupBy(PendingSongLike::songId)
        .mapValues { (_, updates) -> updates.maxBy(PendingSongLike::sequence) }

internal fun resolveLikedStateDuringRemoteReconciliation(
    localLiked: Boolean,
    isLocalSong: Boolean,
    remoteLiked: Boolean,
    pending: PendingSongLike?,
): Boolean =
    when {
        isLocalSong -> localLiked
        pending != null -> pending.liked
        else -> remoteLiked
    }

internal object PendingSongLikeCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun encode(updates: List<PendingSongLike>): String = json.encodeToString(updates)

    fun decode(value: String?): List<PendingSongLike> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<PendingSongLike>>(value)
        }.getOrDefault(emptyList())
    }
}
