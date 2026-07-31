package com.metrolist.music.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackErrorRecoveryPolicyTest {
    @Test
    fun `unspecified IO before client selection retries stream resolution`() {
        assertEquals(
            PlaybackIoRecoveryAction.RETRY_STREAM_RESOLUTION,
            playbackIoRecoveryAction(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                streamClient = null,
            )
        )
    }

    @Test
    fun `WEB_REMIX unspecified IO advances to fallback client`() {
        assertEquals(
            PlaybackIoRecoveryAction.TRY_FALLBACK_CLIENT,
            playbackIoRecoveryAction(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                streamClient = "WEB_REMIX",
            )
        )
    }

    @Test
    fun `unspecified IO does not retry another unsuitable client`() {
        assertEquals(
            PlaybackIoRecoveryAction.DO_NOT_RETRY,
            playbackIoRecoveryAction(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                streamClient = "TVHTML5",
            )
        )
    }

    @Test
    fun `bad HTTP status keeps bounded generic recovery`() {
        assertEquals(
            PlaybackIoRecoveryAction.RETRY_CURRENT_CLIENT,
            playbackIoRecoveryAction(
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                streamClient = "TVHTML5",
            )
        )
    }

    @Test
    fun `unrelated errors use existing handlers`() {
        assertNull(
            playbackIoRecoveryAction(
                PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
                streamClient = "WEB_REMIX",
            )
        )
    }
}
