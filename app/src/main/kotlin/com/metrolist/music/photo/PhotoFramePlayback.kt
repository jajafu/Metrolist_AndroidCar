package com.metrolist.music.photo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

/** Holds URI strings only; image ownership belongs to the visible screen. */
internal class FrameShuffleQueue(uris: List<String>, private val random: Random = Random.Default) {
    private val photos = uris.distinct()
    private val failed = mutableSetOf<String>()
    private var round = emptyList<String>()
    private var index = 0
    private var previous: String? = null
    private var lastReadable: String? = null

    fun markFailed(uri: String) {
        failed += uri
        if (previous == uri) previous = lastReadable
    }
    fun markFailed(uris: Set<String>) { failed += uris }
    fun markReadable(uri: String) {
        lastReadable = uri
        previous = uri
    }

    fun next(): String? {
        while (true) {
            if (index >= round.size) {
                val available = photos.filterNot { it in failed }
                if (available.isEmpty()) return null
                round = available.shuffled(random).toMutableList().apply {
                    if (size > 1 && first() == previous) {
                        val swap = random.nextInt(1, size)
                        this[0] = this[swap].also { this[swap] = this[0] }
                    }
                }
                index = 0
            }
            val next = round[index++]
            if (next !in failed) {
                previous = next
                return next
            }
        }
    }
}

internal data class FrameImage<T>(val uri: String, val image: T)
internal data class FramePlaybackState<T>(
    val current: FrameImage<T>? = null,
    val incoming: FrameImage<T>? = null,
    val exhausted: Boolean = false,
)

/** A pause checkpoint contains no decoded images and can survive a background interval. */
internal class FramePlaybackSession(uris: List<String>, random: Random = Random.Default) {
    val queue = FrameShuffleQueue(uris, random)
    val empty = uris.isEmpty()
    val single = uris.distinct().size == 1
    var currentUri: String? = null
    var pendingUri: String? = null
    var intervalMillis = 0L
    var remainingMillis = 0L
}

/** Runs only inside a foreground screen effect. Cancellation releases both frames. */
internal class PhotoFramePlayback<T>(
    private val load: suspend (String) -> T?,
    private val onUnreadable: suspend (String) -> Unit,
    private val loadTimeoutMillis: Long = 10_000,
    private val transitionMillis: Long = 350,
    private val unavailableUris: () -> Set<String> = { emptySet() },
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    suspend fun play(
        uris: List<String>,
        intervalMillis: Long,
        publish: (FramePlaybackState<T>) -> Unit,
    ) = play(FramePlaybackSession(uris), intervalMillis, publish)

    suspend fun play(
        session: FramePlaybackSession,
        intervalMillis: Long,
        publish: (FramePlaybackState<T>) -> Unit,
    ) {
        if (session.intervalMillis != intervalMillis) {
            session.intervalMillis = intervalMillis
            session.remainingMillis = intervalMillis
        }
        val queue = session.queue
        suspend fun loadPhoto(uri: String): FrameImage<T>? {
            val image = try {
                withTimeoutOrNull(loadTimeoutMillis) { load(uri) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (image != null) {
                queue.markReadable(uri)
                return FrameImage(uri, image)
            }
            queue.markFailed(uri)
            onUnreadable(uri)
            queue.markFailed(unavailableUris())
            return null
        }
        suspend fun loadNext(): FrameImage<T>? {
            while (true) {
                currentCoroutineContext().ensureActive()
                val uri = session.pendingUri ?: queue.next() ?: return null
                session.pendingUri = uri
                loadPhoto(uri)?.let { return it }
                session.pendingUri = null
            }
        }

        var current = session.currentUri?.let { loadPhoto(it) }
        if (current == null) {
            current = loadNext()
            session.pendingUri = null
        }
        if (current == null) {
            session.currentUri = null
            publish(FramePlaybackState(exhausted = !session.empty))
            return
        }
        session.currentUri = current.uri
        publish(FramePlaybackState(current))
        if (session.single) return

        while (true) {
            val started = nowMillis()
            val incoming: FrameImage<T>?
            try {
                incoming = loadNext()
                if (incoming != null) {
                    delay((session.remainingMillis - (nowMillis() - started)).coerceAtLeast(0))
                }
            } finally {
                session.remainingMillis = (session.remainingMillis - (nowMillis() - started)).coerceAtLeast(0)
            }
            if (incoming == null) {
                session.currentUri = null
                publish(FramePlaybackState(exhausted = true))
                return
            }
            if (incoming.uri == current?.uri) {
                session.pendingUri = null
                return
            }
            publish(FramePlaybackState(current, incoming))
            delay(transitionMillis)
            current = incoming
            session.currentUri = incoming.uri
            session.pendingUri = null
            session.remainingMillis = intervalMillis
            publish(FramePlaybackState(current))
        }
    }
}
