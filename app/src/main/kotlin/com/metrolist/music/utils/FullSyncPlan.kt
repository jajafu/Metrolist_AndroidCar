package com.metrolist.music.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin

internal data class FullSyncStep(
    val name: String,
    val execute: suspend () -> SyncStatus,
)

data class FullSyncResult(
    val failedComponents: List<String>,
) {
    val isSuccessful: Boolean
        get() = failedComponents.isEmpty()
}

internal class FullSyncCoordinator(
    private val scope: CoroutineScope,
    private val execute: suspend () -> FullSyncResult,
) {
    private val lock = Any()

    @Volatile
    private var activeSync: Deferred<FullSyncResult>? = null

    val isActive: Boolean
        get() = activeSync?.isActive == true

    fun request(): Deferred<FullSyncResult> =
        synchronized(lock) {
            activeSync?.takeUnless { it.isCompleted }
                ?: scope
                    .async(start = CoroutineStart.LAZY) {
                        execute()
                    }.also { sync ->
                        activeSync = sync
                        sync.invokeOnCompletion {
                            synchronized(lock) {
                                if (activeSync === sync) {
                                    activeSync = null
                                }
                            }
                        }
                        sync.start()
                    }
        }

    suspend fun cancelActive() {
        val sync = synchronized(lock) { activeSync }
        sync?.cancelAndJoin()
    }
}

internal suspend fun executeFullSyncPlan(
    steps: List<FullSyncStep>,
    afterStep: suspend () -> Unit = {},
): FullSyncResult {
    val failedComponents = mutableListOf<String>()

    steps.forEachIndexed { index, step ->
        val status =
            try {
                step.execute()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                SyncStatus.Error(exception.message ?: "Unknown error")
            }

        if (status !is SyncStatus.Completed) {
            failedComponents += step.name
        }
        if (index < steps.lastIndex) {
            afterStep()
        }
    }

    return FullSyncResult(failedComponents)
}

internal fun isFullSyncCoolingDown(
    lastSuccessfulSyncEpoch: Long,
    currentEpoch: Long,
    cooldownSeconds: Long,
): Boolean =
    lastSuccessfulSyncEpoch > 0 &&
        currentEpoch - lastSuccessfulSyncEpoch < cooldownSeconds

internal fun nextLastSuccessfulSyncEpoch(
    previousEpoch: Long,
    currentEpoch: Long,
    result: FullSyncResult,
): Long = if (result.isSuccessful) currentEpoch else previousEpoch
