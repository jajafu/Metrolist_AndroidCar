package com.metrolist.music.viewmodels

internal data class ContentLoadState<T>(
    val content: T? = null,
    val isLoading: Boolean = false,
    val error: Throwable? = null,
) {
    val isWaitingForInitialContent: Boolean
        get() = content == null && error == null

    fun loading(): ContentLoadState<T> =
        copy(
            isLoading = true,
            error = null,
        )

    fun loaded(content: T): ContentLoadState<T> =
        ContentLoadState(content = content)

    fun failed(error: Throwable): ContentLoadState<T> =
        copy(
            isLoading = false,
            error = error,
        )
}
