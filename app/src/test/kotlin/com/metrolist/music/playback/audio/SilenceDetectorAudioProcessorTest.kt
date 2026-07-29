package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SilenceDetectorAudioProcessorTest {
    @Test
    fun `reset prevents silence accumulation from crossing a track boundary`() {
        var notifications = 0
        val processor = SilenceDetectorAudioProcessor(
            minSilenceDurationUs = 2_000,
            onLongSilence = { notifications++ },
        )
        processor.configure(
            AudioProcessor.AudioFormat(
                1_000,
                1,
                C.ENCODING_PCM_16BIT,
            )
        )
        processor.instantModeEnabled = true

        processor.queueInput(silentFrames(1))
        processor.resetTracking()
        processor.queueInput(silentFrames(1))
        assertEquals(0, notifications)

        processor.queueInput(silentFrames(1))
        assertEquals(1, notifications)
    }

    private fun silentFrames(count: Int): ByteBuffer =
        ByteBuffer.allocateDirect(count * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                repeat(count) { putShort(0) }
                flip()
            }
}
