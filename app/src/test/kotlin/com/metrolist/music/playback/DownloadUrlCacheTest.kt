package com.metrolist.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class DownloadUrlCacheTest {
    @Test
    fun `simultaneous resolutions for one song share one result`() {
        val cache = DownloadUrlCache { 1_000L }
        val resolverCalls = AtomicInteger()
        val workerCount = 12
        val workersReady = CountDownLatch(workerCount)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(workerCount)

        try {
            val results =
                List(workerCount) {
                    executor.submit<String> {
                        workersReady.countDown()
                        start.await()
                        cache.getOrResolve("song-id") {
                            resolverCalls.incrementAndGet()
                            Thread.sleep(20)
                            DownloadUrlCacheEntry(
                                url = "https://example.test/audio",
                                expiresAtMs = 2_000L,
                            )
                        }
                    }
                }

            assertTrue(workersReady.await(5, TimeUnit.SECONDS))
            start.countDown()

            assertEquals(
                List(workerCount) { "https://example.test/audio" },
                results.map { it.get(5, TimeUnit.SECONDS) },
            )
            assertEquals(1, resolverCalls.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `expired result is evicted and resolved again`() {
        val currentTimeMs = AtomicLong(1_000L)
        val cache = DownloadUrlCache(currentTimeMs::get)
        val resolverCalls = AtomicInteger()

        fun resolve(): DownloadUrlCacheEntry {
            val resolution = resolverCalls.incrementAndGet()
            return DownloadUrlCacheEntry(
                url = "https://example.test/audio/$resolution",
                expiresAtMs = currentTimeMs.get() + 100L,
            )
        }

        assertEquals("https://example.test/audio/1", cache.getOrResolve("song-id", ::resolve))

        currentTimeMs.set(1_100L)

        assertEquals("https://example.test/audio/2", cache.getOrResolve("song-id", ::resolve))
        assertEquals(2, resolverCalls.get())
    }
}
