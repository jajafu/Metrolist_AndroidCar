package com.metrolist.music.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentLoadStateTest {
    @Test
    fun `initial state waits for content without reporting an error`() {
        val state = ContentLoadState<String>()

        assertTrue(state.isWaitingForInitialContent)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loading clears an old error and preserves cached content`() {
        val cachedContent = "cached"
        val state =
            ContentLoadState(
                content = cachedContent,
                error = IllegalStateException("old failure"),
            )

        val loading = state.loading()

        assertEquals(cachedContent, loading.content)
        assertTrue(loading.isLoading)
        assertNull(loading.error)
        assertFalse(loading.isWaitingForInitialContent)
    }

    @Test
    fun `failure ends loading and preserves cached content`() {
        val failure = IllegalStateException("network unavailable")
        val state = ContentLoadState(content = "cached").loading().failed(failure)

        assertEquals("cached", state.content)
        assertFalse(state.isLoading)
        assertSame(failure, state.error)
    }

    @Test
    fun `initial failure stops the waiting state so the UI can show retry`() {
        val state =
            ContentLoadState<String>()
                .loading()
                .failed(IllegalStateException("network unavailable"))

        assertNull(state.content)
        assertFalse(state.isLoading)
        assertFalse(state.isWaitingForInitialContent)
    }

    @Test
    fun `success replaces content and clears loading and error`() {
        val state =
            ContentLoadState(content = "old")
                .loading()
                .failed(IllegalStateException("temporary"))
                .loading()
                .loaded("new")

        assertEquals("new", state.content)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isWaitingForInitialContent)
    }
}
