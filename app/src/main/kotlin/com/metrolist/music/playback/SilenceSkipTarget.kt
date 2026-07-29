/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

internal data class SilenceSkipTarget(
    val mediaId: String,
    val mediaItemIndex: Int,
)

internal fun isCurrentSilenceSkipTarget(
    expected: SilenceSkipTarget,
    currentMediaId: String?,
    currentMediaItemIndex: Int,
): Boolean =
    expected.mediaId == currentMediaId &&
        expected.mediaItemIndex == currentMediaItemIndex
