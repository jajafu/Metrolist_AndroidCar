package com.metrolist.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLoadMoreRetryTest {
    @Test
    fun `default policy allows one immediate attempt and three bounded retries`() {
        val policy = AutoLoadMoreRetryPolicy()

        assertEquals(0L, policy.delayBeforeAttempt(1))
        assertEquals(1_000L, policy.delayBeforeAttempt(2))
        assertEquals(2_000L, policy.delayBeforeAttempt(3))
        assertEquals(4_000L, policy.delayBeforeAttempt(4))
        assertNull(policy.delayBeforeAttempt(5))
    }

    @Test
    fun `retry delay never exceeds configured maximum`() {
        val policy =
            AutoLoadMoreRetryPolicy(
                maxAttempts = 6,
                initialDelayMillis = 3_000L,
                maximumDelayMillis = 5_000L,
            )

        assertEquals(3_000L, policy.delayBeforeAttempt(2))
        assertEquals(5_000L, policy.delayBeforeAttempt(3))
        assertEquals(5_000L, policy.delayBeforeAttempt(6))
    }

    @Test
    fun `tracker stops after the configured attempt limit`() {
        val tracker = AutoLoadMoreRetryTracker()

        assertEquals(1_000L, tracker.recordFailure())
        assertEquals(2_000L, tracker.recordFailure())
        assertEquals(4_000L, tracker.recordFailure())
        assertNull(tracker.recordFailure())
        assertEquals(4, tracker.failedAttempts)
    }

    @Test
    fun `manual retry reset restores the initial attempt`() {
        val tracker = AutoLoadMoreRetryTracker()
        tracker.recordFailure()
        tracker.recordFailure()

        tracker.reset()

        assertEquals(0, tracker.failedAttempts)
        assertEquals(1, tracker.nextAttempt)
    }

    @Test
    fun `continuation page remains available without similar content`() {
        assertTrue(
            canRequestMoreQueueItems(
                hasNextPage = true,
                similarContentEnabled = false,
                hasFallbackSeed = false,
            ),
        )
    }

    @Test
    fun `exhausted continuation uses similar-content fallback when a seed exists`() {
        assertTrue(
            canRequestMoreQueueItems(
                hasNextPage = false,
                similarContentEnabled = true,
                hasFallbackSeed = true,
            ),
        )
    }

    @Test
    fun `exhausted continuation stops when similar content is disabled`() {
        assertFalse(
            canRequestMoreQueueItems(
                hasNextPage = false,
                similarContentEnabled = false,
                hasFallbackSeed = true,
            ),
        )
    }

    @Test
    fun `queue end detection follows shuffled playback order`() {
        val shuffledNext = mapOf(4 to 9, 9 to 1, 1 to -1)

        assertTrue(
            isNearQueueEndByPlaybackOrder(
                currentIndex = 4,
                threshold = 3,
            ) { shuffledNext[it] ?: -1 },
        )
    }

    @Test
    fun `physical tail is not near the end when shuffle has more tracks`() {
        val shuffledNext = mapOf(8 to 2, 2 to 5, 5 to 7, 7 to -1)

        assertFalse(
            isNearQueueEndByPlaybackOrder(
                currentIndex = 8,
                threshold = 3,
            ) { shuffledNext[it] ?: -1 },
        )
    }

    @Test
    fun `non-shuffled playback triggers at the configured threshold`() {
        assertTrue(
            isNearQueueEndByPlaybackOrder(
                currentIndex = 7,
                threshold = 3,
            ) { index -> if (index < 9) index + 1 else -1 },
        )
    }
}
