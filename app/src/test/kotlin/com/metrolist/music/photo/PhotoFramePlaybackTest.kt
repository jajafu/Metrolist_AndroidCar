package com.metrolist.music.photo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhotoFramePlaybackTest {
    @Test
    fun `shuffle is unique per round and avoids boundary repeats`() {
        repeat(20) { seed ->
            val source = listOf("a", "b", "c", "d")
            val queue = FrameShuffleQueue(source + "a", Random(seed))
            var previous: String? = null
            repeat(10) {
                val round = List(source.size) { queue.next()!! }
                assertEquals(source.toSet(), round.toSet())
                assertNotEquals(previous, round.first())
                previous = round.last()
            }
        }
    }

    @Test
    fun `failed URIs are not retried indefinitely`() {
        val queue = FrameShuffleQueue(listOf("a", "b"))
        queue.markFailed("a")
        assertEquals("b", queue.next())
        queue.markFailed("b")
        assertNull(queue.next())
    }

    @Test
    fun `previous and next walk the playback history`() {
        val queue = FrameShuffleQueue(listOf("a", "b", "c"), Random(1))
        val first = queue.next()
        val second = queue.next()

        assertEquals(first, queue.previous())
        assertEquals(second, queue.next())
    }

    @Test
    fun `empty pool does not load or start a timer`() = runBlocking {
        val engine = PhotoFramePlayback<String>(load = { error("Unexpected image request") }, onUnreadable = {})
        var state: FramePlaybackState<String>? = null
        engine.play(emptyList(), 60_000) { state = it }
        assertNull(state!!.current)
        assertFalse(state.exhausted)
    }

    @Test
    fun `single photo is loaded once without a timer`() = runBlocking {
        var loads = 0
        val engine = PhotoFramePlayback(load = { uri: String -> loads++; uri }, onUnreadable = {})
        var state: FramePlaybackState<String>? = null
        engine.play(listOf("a"), 60_000) { state = it }
        assertEquals(1, loads)
        assertEquals("a", state!!.current!!.uri)
        assertNull(state.incoming)
    }

    @Test
    fun `manual next command advances without waiting for the interval`() = runBlocking {
        val session = FramePlaybackSession(listOf("a", "b", "c"), Random(1))
        val firstShown = CompletableDeferred<Unit>()
        val secondShown = CompletableDeferred<Unit>()
        val previousShown = CompletableDeferred<Unit>()
        val shown = mutableListOf<String>()
        val engine = PhotoFramePlayback<String>(
            load = { it },
            onUnreadable = {},
            transitionMillis = 0,
        )
        val job = launch {
            engine.play(session, 60_000) { state ->
                state.current?.let { frame ->
                    if (shown.lastOrNull() != frame.uri) {
                        shown += frame.uri
                        if (shown.size == 1) firstShown.complete(Unit)
                        if (shown.size == 2) secondShown.complete(Unit)
                        if (shown.size == 3) previousShown.complete(Unit)
                    }
                }
            }
        }

        firstShown.await()
        session.request(FramePlaybackCommand.NEXT)
        secondShown.await()
        session.request(FramePlaybackCommand.PREVIOUS)
        previousShown.await()
        job.cancelAndJoin()

        assertNotEquals(shown[0], shown[1])
        assertEquals(shown[0], shown[2])
    }

    @Test
    fun `all unreadable images terminate with an empty error state`() = runBlocking {
        val failures = mutableListOf<String>()
        val engine = PhotoFramePlayback<String>(load = { null }, onUnreadable = { failures += it })
        var state: FramePlaybackState<String>? = null
        engine.play(listOf("a", "b"), 0) { state = it }
        assertEquals(setOf("a", "b"), failures.toSet())
        assertEquals(2, failures.size)
        assertTrue(state!!.exhausted)
        assertNull(state.current)
    }

    @Test
    fun `broken photo does not prevent valid photos playing`() = runBlocking {
        val failures = mutableListOf<String>()
        val displayed = mutableSetOf<String>()
        val engine = PhotoFramePlayback(
            load = { uri: String -> uri.takeUnless { it == "bad" } },
            onUnreadable = { failures += it },
            transitionMillis = 0,
        )
        try {
            engine.play(listOf("a", "bad", "b"), 0) {
                it.current?.let { image -> displayed += image.uri }
                if (displayed.size == 2) throw CancellationException("Test complete")
            }
        } catch (_: CancellationException) {
            // Stop the otherwise infinite slideshow once both readable images were displayed.
        }
        assertEquals(setOf("a", "b"), displayed)
        assertTrue(failures.all { it == "bad" })
    }

    @Test
    fun `screen cancellation cancels pending decode without reporting a bad photo`() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val failures = mutableListOf<String>()
        val engine = PhotoFramePlayback<String>(
            load = { entered.complete(Unit); awaitCancellation() },
            onUnreadable = { failures += it },
        )
        val job = launch { engine.play(listOf("a"), 0) { error("Unexpected completed load") } }
        entered.await()
        job.cancelAndJoin()
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `pause checkpoints current pending and remaining time without reshuffling`() = runBlocking {
        val session = FramePlaybackSession(listOf("a", "b", "c"))
        val preloading = CompletableDeferred<Unit>()
        val requested = mutableListOf<String>()
        var time = 0L
        val engine = PhotoFramePlayback(
            load = { uri: String ->
                requested += uri
                if (requested.size == 2) {
                    time = 250L
                    preloading.complete(Unit)
                    awaitCancellation()
                }
                uri
            },
            onUnreadable = { error("Cancellation is not a failed image") },
            nowMillis = { time },
        )
        val job = launch { engine.play(session, 1_000) {} }
        preloading.await()
        job.cancelAndJoin()
        assertEquals(requested[0], session.currentUri)
        assertEquals(requested[1], session.pendingUri)
        assertEquals(750L, session.remainingMillis)

        val resumed = mutableListOf<String>()
        val resumeEngine = PhotoFramePlayback(
            load = { uri: String -> resumed += uri; uri },
            onUnreadable = {},
        )
        try {
            resumeEngine.play(session, 0) {
                if (it.incoming != null) throw CancellationException("Both saved frames restored")
            }
        } catch (_: CancellationException) {
            assertEquals(requested, resumed)
        }
        assertTrue(session.queue.next() !in requested)
    }

    @Test
    fun `a disconnected source skips its remaining URIs without repeated timeouts`() = runBlocking {
        val requested = mutableListOf<String>()
        val uris = List(1_000) { "usb-$it" }
        val engine = PhotoFramePlayback<String>(
            load = { requested += it; null },
            onUnreadable = {},
            unavailableUris = { uris.toSet() },
        )
        engine.play(uris, 60_000) { assertTrue(it.exhausted) }
        assertEquals(1, requested.size)
    }

    @Test
    fun `load timeout skips a stalled provider and terminates`() = runBlocking {
        val failed = mutableListOf<String>()
        val engine = PhotoFramePlayback<String>(
            load = { awaitCancellation() },
            onUnreadable = { failed += it },
            loadTimeoutMillis = 10,
        )
        engine.play(listOf("stalled"), 0) { assertTrue(it.exhausted) }
        assertEquals(listOf("stalled"), failed)
    }

    @Test
    fun `a broken last photo in a round cannot stall two readable photos`() = runBlocking {
        repeat(20) { seed ->
            val shown = mutableListOf<String>()
            val engine = PhotoFramePlayback(
                load = { uri: String -> uri.takeUnless { it == "bad" } },
                onUnreadable = {},
                transitionMillis = 0,
            )
            val session = FramePlaybackSession(listOf("a", "b", "bad"), Random(seed))
            try {
                engine.play(session, 0) { frame ->
                    if (frame.incoming == null && frame.current != null) {
                        shown += frame.current.uri
                        if (shown.size == 40) throw CancellationException("Test complete")
                    }
                }
            } catch (_: CancellationException) {
                // Every seed must reach 40 frames, not silently return after a repeated preload.
            }
            assertEquals("Slideshow stopped for seed $seed", 40, shown.size)
            assertTrue(shown.zipWithNext().all { (first, second) -> first != second })
        }
    }
}
