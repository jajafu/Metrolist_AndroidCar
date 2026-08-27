package com.metrolist.music.photo

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoFrameManifestTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `manifest round trip includes empty scanned folders and source duplicates`() {
        val store = PhotoFrameManifest(temporary.newFile("index.json"))
        val first = FramePhoto("content://photos/1", "content://folders/a")
        val second = FramePhoto("content://photos/1", "content://folders/b")
        val expected = FrameManifest(photos = listOf(first, second), scannedFolders = setOf("content://folders/a", "content://folders/b", "content://folders/empty"))
        store.write(expected.copy(photos = listOf(first, first, second)))

        assertEquals(expected, store.read().manifest)
        assertFalse(store.read().damaged)
    }

    @Test
    fun `corruption and unknown versions recover to a rebuildable index`() {
        val file = temporary.newFile("index.json")
        val store = PhotoFrameManifest(file)
        for (contents in listOf("broken json", "{\"schemaVersion\":99}", "{\"schemaVersion\":1,\"photos\":[{\"uri\":\"\",\"sourceUri\":\"folder\"}]}")) {
            file.writeText(contents)
            assertTrue(store.read().damaged)
            assertTrue(store.read().manifest.photos.isEmpty())
        }
        store.write(FrameManifest())
        assertFalse(store.read().damaged)
    }

    @Test
    fun `failed atomic replacement preserves previous complete index and cleans temporary file`() {
        val file = temporary.newFile("index.json")
        val previous = FrameManifest(photos = listOf(FramePhoto("content://photos/old", "folder")))
        PhotoFrameManifest(file).write(previous)
        val store = PhotoFrameManifest(file) { _, _ -> throw IOException("Injected rename failure") }
        try {
            store.write(FrameManifest(photos = listOf(FramePhoto("content://photos/new", "folder"))))
            throw AssertionError("Expected failed replacement")
        } catch (_: IOException) {
            assertEquals(previous, store.read().manifest)
            assertEquals(listOf("index.json"), temporary.root.listFiles().orEmpty().map { it.name })
        }
    }

    @Test
    fun `uncommitted temporary files do not replace the last index`() {
        val file = temporary.newFile("index.json")
        val manifest = FrameManifest(photos = listOf(FramePhoto("content://photos/1", "folder")))
        PhotoFrameManifest(file).write(manifest)
        temporary.newFile("index.json.interrupted.tmp").writeText("partial new file")

        assertEquals(manifest, PhotoFrameManifest(file).read().manifest)
    }

    @Test
    fun `merge deduplicates URIs and removing one source retains another owner`() {
        val picked = FrameSource("content://photos/1", "one", FrameSelectionType.PICKED_PHOTO)
        val folder = FrameSource("content://folders/a", "a", FrameSelectionType.FOLDER)
        val photos = listOf(FramePhoto(picked.uri, folder.uri), FramePhoto("content://photos/2", folder.uri))

        assertEquals(listOf(picked.uri, "content://photos/2"), mergeFramePhotos(listOf(picked, folder), photos).map { it.uri })
        assertEquals(photos, mergeFramePhotos(listOf(folder), photos))
        assertEquals(listOf(FramePhoto(picked.uri, picked.uri)), mergeFramePhotos(listOf(picked, folder.copy(needsPermission = true)), photos))
    }
}
