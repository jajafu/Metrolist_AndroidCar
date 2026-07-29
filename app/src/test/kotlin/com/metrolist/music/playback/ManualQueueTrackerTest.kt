package com.metrolist.music.playback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ManualQueueTrackerTest {
    @Test
    fun `repeated play next insertions remain FIFO without shuffle`() {
        val tracker = ManualQueueTracker()

        assertEquals(3, tracker.insertionIndex(currentIndex = 2, mediaItemCount = 6))
        tracker.recordInsertion(listOf("first"))
        assertEquals(4, tracker.insertionIndex(currentIndex = 2, mediaItemCount = 7))
        tracker.recordInsertion(listOf("second", "third"))

        assertEquals(6, tracker.insertionIndex(currentIndex = 2, mediaItemCount = 9))
        assertEquals(3, tracker.pendingCount)
    }

    @Test
    fun `shuffle keeps all manual items ahead of automatic queue`() {
        val result =
            buildManualPriorityShuffleOrder(
                baseOrder = intArrayOf(0, 4, 2, 6, 1, 5, 3),
                currentIndex = 2,
                totalCount = 7,
                pendingManualCount = 3,
            )

        assertArrayEquals(intArrayOf(0, 2, 3, 4, 5, 6, 1), result)
    }

    @Test
    fun `playing manual items consumes only the completed priority`() {
        val tracker = ManualQueueTracker()
        tracker.recordInsertion(listOf("first", "second", "third"))

        tracker.onTransition(previousIndex = 2, newIndex = 3, newMediaId = "first")
        assertEquals(2, tracker.pendingCount)
        tracker.onTransition(previousIndex = 3, newIndex = 4, newMediaId = "second")

        assertEquals(1, tracker.pendingCount)
        assertEquals(6, tracker.insertionIndex(currentIndex = 4, mediaItemCount = 9))
    }

    @Test
    fun `repeat of current item keeps manual priorities`() {
        val tracker = ManualQueueTracker()
        tracker.recordInsertion(listOf("first", "second"))

        tracker.onTransition(previousIndex = 2, newIndex = 2, newMediaId = "current")

        assertEquals(2, tracker.pendingCount)
    }

    @Test
    fun `removal from manual block decrements while preserving order`() {
        val tracker = ManualQueueTracker()
        tracker.recordInsertion(listOf("first", "second", "third"))

        tracker.reconcileUpcoming(listOf("first", "third", "automatic"))

        assertEquals(2, tracker.pendingCount)
    }

    @Test
    fun `queue replacement and unrelated transition reset manual priorities`() {
        val tracker = ManualQueueTracker()
        tracker.recordInsertion(listOf("first", "second"))

        tracker.onTransition(previousIndex = 2, newIndex = 5, newMediaId = "automatic")
        assertEquals(0, tracker.pendingCount)

        tracker.recordInsertion(listOf("new"))
        tracker.reset()
        assertEquals(0, tracker.pendingCount)
    }
}
