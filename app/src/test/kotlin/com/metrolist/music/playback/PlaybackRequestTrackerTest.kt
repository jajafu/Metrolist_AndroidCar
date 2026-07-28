package com.metrolist.music.playback

import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRequestTrackerTest {
    @Test
    fun `new request invalidates previous request`() {
        val tracker = PlaybackRequestTracker()
        val first = tracker.begin()

        val second = tracker.begin()

        assertFalse(tracker.isActive(first))
        assertTrue(tracker.isActive(second))
    }

    @Test
    fun `slower queue A cannot replace faster queue B`() =
        runBlocking {
            val tracker = PlaybackRequestTracker()
            val appliedQueues = mutableListOf<String>()
            val queueA = tracker.begin()

            val slowA =
                launch {
                    delay(100)
                    if (tracker.isActive(queueA)) {
                        appliedQueues += "A"
                    }
                }

            delay(10)
            val queueB = tracker.begin()
            val fastB =
                launch {
                    delay(10)
                    if (tracker.isActive(queueB)) {
                        appliedQueues += "B"
                    }
                }

            joinAll(slowA, fastB)

            assertEquals(listOf("B"), appliedQueues)
        }
}
