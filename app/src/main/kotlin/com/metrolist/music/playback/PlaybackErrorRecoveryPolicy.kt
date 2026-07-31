/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import androidx.media3.common.PlaybackException

internal enum class PlaybackIoRecoveryAction {
    RETRY_CURRENT_CLIENT,
    RETRY_STREAM_RESOLUTION,
    TRY_FALLBACK_CLIENT,
    DO_NOT_RETRY,
}

internal fun playbackIoRecoveryAction(
    errorCode: Int,
    streamClient: String?,
): PlaybackIoRecoveryAction? =
    when (errorCode) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            PlaybackIoRecoveryAction.RETRY_CURRENT_CLIENT

        PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
            when (streamClient) {
                null -> PlaybackIoRecoveryAction.RETRY_STREAM_RESOLUTION
                "WEB_REMIX" -> PlaybackIoRecoveryAction.TRY_FALLBACK_CLIENT
                else -> PlaybackIoRecoveryAction.DO_NOT_RETRY
            }

        else -> null
    }
