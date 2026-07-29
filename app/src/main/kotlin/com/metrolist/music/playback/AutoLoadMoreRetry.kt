/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

sealed interface AutoLoadMoreState {
    data object Idle : AutoLoadMoreState

    data class Loading(
        val attempt: Int,
    ) : AutoLoadMoreState

    data class Retrying(
        val attempt: Int,
        val delayMillis: Long,
    ) : AutoLoadMoreState

    data class WaitingForNetwork(
        val attempt: Int,
    ) : AutoLoadMoreState

    data class Failed(
        val attempts: Int,
    ) : AutoLoadMoreState
}

internal class AutoLoadMoreRetryPolicy(
    val maxAttempts: Int = 4,
    private val initialDelayMillis: Long = 1_000L,
    private val maximumDelayMillis: Long = 4_000L,
) {
    init {
        require(maxAttempts > 0)
        require(initialDelayMillis > 0)
        require(maximumDelayMillis >= initialDelayMillis)
    }

    fun delayBeforeAttempt(attempt: Int): Long? {
        if (attempt !in 1..maxAttempts) return null
        if (attempt == 1) return 0L

        var delayMillis = initialDelayMillis
        repeat(attempt - 2) {
            delayMillis =
                if (delayMillis >= maximumDelayMillis / 2) {
                    maximumDelayMillis
                } else {
                    delayMillis * 2
                }
        }
        return delayMillis.coerceAtMost(maximumDelayMillis)
    }
}

internal class AutoLoadMoreRetryTracker(
    private val policy: AutoLoadMoreRetryPolicy = AutoLoadMoreRetryPolicy(),
) {
    var failedAttempts: Int = 0
        private set

    val nextAttempt: Int
        get() = failedAttempts + 1

    fun recordFailure(): Long? {
        failedAttempts++
        return policy.delayBeforeAttempt(nextAttempt)
    }

    fun reset() {
        failedAttempts = 0
    }
}

internal fun canRequestMoreQueueItems(
    hasNextPage: Boolean,
    similarContentEnabled: Boolean,
    hasFallbackSeed: Boolean,
): Boolean = hasNextPage || (similarContentEnabled && hasFallbackSeed)
