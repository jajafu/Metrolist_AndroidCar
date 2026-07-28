package com.metrolist.music.utils

import kotlinx.coroutines.CancellationException

internal data class FullSyncStep(
    val name: String,
    val execute: suspend () -> SyncStatus,
)

internal data class FullSyncPlanResult(
    val failedComponents: List<String>,
) {
    val isSuccessful: Boolean
        get() = failedComponents.isEmpty()
}

internal suspend fun executeFullSyncPlan(
    steps: List<FullSyncStep>,
    afterStep: suspend () -> Unit = {},
): FullSyncPlanResult {
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

    return FullSyncPlanResult(failedComponents)
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
    result: FullSyncPlanResult,
): Long = if (result.isSuccessful) currentEpoch else previousEpoch
