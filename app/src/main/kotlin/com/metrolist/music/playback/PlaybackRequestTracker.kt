/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

internal class PlaybackRequestTracker {
    @JvmInline
    value class Token internal constructor(
        private val generation: Long,
    )

    private var activeGeneration = 0L

    fun begin(): Token = Token(++activeGeneration)

    fun isActive(token: Token): Boolean = token == Token(activeGeneration)
}
