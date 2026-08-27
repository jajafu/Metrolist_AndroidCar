package com.metrolist.music.photo

import java.io.FileNotFoundException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameFolderScanTest {
    @Test
    fun `recursive scan queries each directory once and ignores videos`() = runBlocking {
        val visits = mutableListOf<String>()
        val progress = mutableListOf<Int>()
        val entries = mapOf(
            "root" to listOf(directory("child"), photo("one"), FrameDocument("video", "movie", "video/mp4")),
            "child" to listOf(directory("root"), photo("one"), photo("two")),
        )
        val photos = scanFrameFolder("source", "root", { uri -> visits.add(uri); entries.getValue(uri) }, progress::add)

        assertEquals(listOf("root", "child"), visits)
        assertEquals(listOf(FramePhoto("one", "source"), FramePhoto("two", "source")), photos)
        assertEquals(2, progress.last())
    }

    @Test
    fun `provider disconnect propagates instead of returning a partial scan`() = runBlocking {
        try {
            scanFrameFolder("source", "root", { uri ->
                if (uri == "root") listOf(photo("one"), directory("child")) else throw FileNotFoundException("USB disconnected")
            }, {})
            throw AssertionError("Must not return partial photos")
        } catch (_: FileNotFoundException) {
            // The catalog can retain the old complete index and show this source as unavailable.
        }
    }

    @Test
    fun `scan cancellation is never converted into an empty success`() = runBlocking {
        var cancelled = false
        try {
            scanFrameFolder("source", "root", { throw CancellationException("User cancelled") }, {})
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }

    private fun directory(uri: String) = FrameDocument(uri, uri, "vnd.android.document/directory")
    private fun photo(uri: String) = FrameDocument(uri, uri, "image/jpeg")
}
