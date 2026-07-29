/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

internal fun isUgcPodcastOrUnknown(musicVideoType: String?): Boolean =
    musicVideoType.isNullOrBlank() ||
        musicVideoType.equals("MUSIC_VIDEO_TYPE_UGC", ignoreCase = true) ||
        musicVideoType.contains("PODCAST", ignoreCase = true)

internal fun shouldSkipWebRemixValidation(
    clientName: String,
    webRemixPreviouslyFailed: Boolean,
    musicVideoType: String?,
): Boolean =
    clientName == "WEB_REMIX" &&
        !webRemixPreviouslyFailed &&
        !isUgcPodcastOrUnknown(musicVideoType)

internal fun shouldSkipFailedWebRemixClient(
    clientName: String,
    webRemixPreviouslyFailed: Boolean,
): Boolean = clientName == "WEB_REMIX" && webRemixPreviouslyFailed
