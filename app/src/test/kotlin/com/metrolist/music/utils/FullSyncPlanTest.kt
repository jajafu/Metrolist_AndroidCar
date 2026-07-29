package com.metrolist.music.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullSyncPlanTest {
    @Test
    fun `successful plan records every component as completed`() =
        runBlocking {
            val result =
                executeFullSyncPlan(
                    listOf(
                        FullSyncStep("liked songs") { SyncStatus.Completed },
                        FullSyncStep("playlists") { SyncStatus.Completed },
                    ),
                )

            assertTrue(result.isSuccessful)
            assertTrue(result.failedComponents.isEmpty())
            assertEquals(200L, nextLastSuccessfulSyncEpoch(100L, 200L, result))
        }

    @Test
    fun `partial failure preserves previous success time for immediate retry`() =
        runBlocking {
            val executedComponents = mutableListOf<String>()
            val result =
                executeFullSyncPlan(
                    listOf(
                        FullSyncStep("liked songs") {
                            executedComponents += "liked songs"
                            SyncStatus.Completed
                        },
                        FullSyncStep("playlists") {
                            executedComponents += "playlists"
                            SyncStatus.Error("network error")
                        },
                        FullSyncStep("artists") {
                            executedComponents += "artists"
                            SyncStatus.Completed
                        },
                    ),
                )

            assertFalse(result.isSuccessful)
            assertEquals(listOf("playlists"), result.failedComponents)
            assertEquals(listOf("liked songs", "playlists", "artists"), executedComponents)
            assertEquals(0L, nextLastSuccessfulSyncEpoch(0L, 200L, result))
            assertFalse(isFullSyncCoolingDown(0L, 201L, 1_800L))
        }

    @Test
    fun `cooldown applies only before its boundary`() {
        assertTrue(isFullSyncCoolingDown(1_000L, 2_799L, 1_800L))
        assertFalse(isFullSyncCoolingDown(1_000L, 2_800L, 1_800L))
    }

    @Test
    fun `failed pending like retry prevents full sync from advancing success time`() =
        runBlocking {
            val result =
                executeFullSyncPlan(
                    listOf(
                        FullSyncStep("pending song likes") {
                            SyncStatus.Error("Pending song likes remain")
                        },
                        FullSyncStep("liked songs") { SyncStatus.Completed },
                    ),
                )

            assertFalse(result.isSuccessful)
            assertEquals(listOf("pending song likes"), result.failedComponents)
            assertEquals(100L, nextLastSuccessfulSyncEpoch(100L, 200L, result))
        }
}
