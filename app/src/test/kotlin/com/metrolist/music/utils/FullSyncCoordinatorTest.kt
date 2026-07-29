package com.metrolist.music.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class FullSyncCoordinatorTest {
    @Test
    fun `auto sync and pull refresh share active full sync`() =
        runBlocking {
            val executionCount = AtomicInteger()
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val coordinator =
                FullSyncCoordinator(scope) {
                    executionCount.incrementAndGet()
                    started.complete(Unit)
                    release.await()
                    FullSyncResult(emptyList())
                }

            try {
                val autoSync = coordinator.request()
                started.await()
                val pullRefresh = coordinator.request()

                assertSame(autoSync, pullRefresh)
                assertEquals(1, executionCount.get())

                release.complete(Unit)
                assertEquals(autoSync.await(), pullRefresh.await())
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun `repeated pulls join current sync and a later pull starts a new sync`() =
        runBlocking {
            val executionCount = AtomicInteger()
            val firstStarted = CompletableDeferred<Unit>()
            val firstRelease = CompletableDeferred<Unit>()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val coordinator =
                FullSyncCoordinator(scope) {
                    if (executionCount.incrementAndGet() == 1) {
                        firstStarted.complete(Unit)
                        firstRelease.await()
                    }
                    FullSyncResult(emptyList())
                }

            try {
                val firstPull = coordinator.request()
                firstStarted.await()
                val repeatedPull = coordinator.request()

                assertSame(firstPull, repeatedPull)
                firstRelease.complete(Unit)
                firstPull.await()

                val laterPull = coordinator.request()
                assertNotSame(firstPull, laterPull)
                laterPull.await()
                assertEquals(2, executionCount.get())
            } finally {
                scope.cancel()
            }
        }
}
