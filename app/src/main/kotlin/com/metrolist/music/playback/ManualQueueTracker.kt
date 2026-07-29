package com.metrolist.music.playback

internal class ManualQueueTracker {
    private var pendingMediaIds = emptyList<String>()

    val pendingCount: Int
        get() = pendingMediaIds.size

    fun reset() {
        pendingMediaIds = emptyList()
    }

    fun insertionIndex(
        currentIndex: Int,
        mediaItemCount: Int,
    ): Int =
        (currentIndex + pendingCount + 1)
            .coerceIn(0, mediaItemCount)

    fun recordInsertion(mediaIds: List<String>) {
        if (mediaIds.isNotEmpty()) {
            pendingMediaIds = pendingMediaIds + mediaIds
        }
    }

    fun onTransition(
        previousIndex: Int,
        newIndex: Int,
        newMediaId: String?,
    ) {
        if (pendingMediaIds.isEmpty()) return
        if (previousIndex == newIndex && previousIndex >= 0) return
        if (newMediaId == null) {
            reset()
            return
        }

        val consumedIndex = pendingMediaIds.indexOf(newMediaId)
        if (consumedIndex < 0) {
            reset()
        } else {
            pendingMediaIds = pendingMediaIds.drop(consumedIndex + 1)
        }
    }

    fun reconcileUpcoming(upcomingMediaIds: List<String>) {
        if (pendingMediaIds.isEmpty()) return

        val retained = mutableListOf<String>()
        var expectedStart = 0
        for (actualId in upcomingMediaIds) {
            while (expectedStart < pendingMediaIds.size && pendingMediaIds[expectedStart] != actualId) {
                expectedStart++
            }
            if (expectedStart == pendingMediaIds.size) break

            retained += pendingMediaIds[expectedStart]
            expectedStart++
            if (expectedStart == pendingMediaIds.size) break
        }
        pendingMediaIds = retained
    }
}

internal fun buildManualPriorityShuffleOrder(
    baseOrder: IntArray,
    currentIndex: Int,
    totalCount: Int,
    pendingManualCount: Int,
): IntArray {
    if (totalCount <= 0 || currentIndex !in 0 until totalCount) return IntArray(0)

    val normalizedOrder = ArrayList<Int>(totalCount)
    val included = BooleanArray(totalCount)
    baseOrder.forEach { index ->
        if (index in 0 until totalCount && !included[index]) {
            normalizedOrder += index
            included[index] = true
        }
    }
    for (index in 0 until totalCount) {
        if (!included[index]) {
            normalizedOrder += index
        }
    }
    val manualIndices =
        (
            currentIndex + 1 until
                (currentIndex + 1 + pendingManualCount).coerceAtMost(totalCount)
        ).toList()
    val manualSet = manualIndices.toSet()
    val currentPosition = normalizedOrder.indexOf(currentIndex)
    val history =
        normalizedOrder
            .take(currentPosition.coerceAtLeast(0))
            .filterNot { it in manualSet }
    val future =
        normalizedOrder
            .drop((currentPosition + 1).coerceAtLeast(0))
            .filterNot { it in manualSet || it == currentIndex }

    return (history + currentIndex + manualIndices + future).toIntArray()
}
