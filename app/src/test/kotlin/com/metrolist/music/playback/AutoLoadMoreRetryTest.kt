package com.metrolist.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
