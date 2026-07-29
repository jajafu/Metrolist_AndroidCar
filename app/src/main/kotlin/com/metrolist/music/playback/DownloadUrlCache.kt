/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import java.util.concurrent.ConcurrentHashMap

internal data class DownloadUrlCacheEntry(
    val url: String,
    val expiresAtMs: Long,
)

internal class DownloadUrlCache(
    private val currentTimeMs: () -> Long = System::currentTimeMillis,
) {
    private val entries = ConcurrentHashMap<String, DownloadUrlCacheEntry>()

    fun getOrResolve(
        mediaId: String,
        resolver: () -> DownloadUrlCacheEntry,
    ): String {
        entries.compute(mediaId) { _, cachedEntry ->
            cachedEntry?.takeIf { it.expiresAtMs > currentTimeMs() }
        }?.let { return it.url }

        return entries.computeIfAbsent(mediaId) { resolver() }.url
    }
}
