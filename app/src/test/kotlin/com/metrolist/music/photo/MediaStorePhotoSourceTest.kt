package com.metrolist.music.photo

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MediaStorePhotoSourceTest {
    private val context = Application()

    @Test
    fun `permission request follows platform media model`() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            requiredMediaStorePhotoPermissions(Build.VERSION_CODES.S_V2),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
            requiredMediaStorePhotoPermissions(Build.VERSION_CODES.TIRAMISU),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
            requiredMediaStorePhotoPermissions(Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
        )
    }

    @Test
    fun `partial visual access is sufficient on Android 14`() {
        val access = hasMediaStorePhotoAccess(context, Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { permission ->
            if (permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) PackageManager.PERMISSION_GRANTED
            else PackageManager.PERMISSION_DENIED
        }

        assertTrue(access)
    }

    @Test
    fun `photo access is rejected when matching permission is absent`() {
        assertFalse(
            hasMediaStorePhotoAccess(context, Build.VERSION_CODES.TIRAMISU) {
                PackageManager.PERMISSION_DENIED
            },
        )
    }

    @Test
    fun `pages append without duplicating provider rows`() {
        val first = MediaStorePhoto("content://media/1", "one.jpg")
        val second = MediaStorePhoto("content://media/2", "two.jpg")

        assertEquals(listOf(first, second), mergeMediaStorePhotos(listOf(first), listOf(first, second)))
    }

    @Test
    fun `diagnostics match primary and exact removable MediaStore names`() {
        val primary = StorageVolumeSnapshot("Internal", "external_primary", null, "mounted", "/storage/emulated/0", true, false)
        val usb = StorageVolumeSnapshot("USB 4", "usb3", "ABCD-1234", "mounted", "/storage/USB3", false, true)
        val mediaVolumes = listOf(
            MediaStoreVolume("external_primary", MediaStoreVolumeKind.PRIMARY),
            MediaStoreVolume("usb3", MediaStoreVolumeKind.REMOVABLE),
        )

        assertEquals(
            mapOf("external_primary" to primary, "usb3" to usb),
            resolveStorageVolumes(mediaVolumes, listOf(primary, usb)),
        )
    }

    @Test
    fun `diagnostics use sole removable storage when vendor names differ`() {
        val usb = StorageVolumeSnapshot("USB 4", null, null, "mounted", null, false, true)

        assertEquals(
            mapOf("usb3" to usb),
            resolveStorageVolumes(
                listOf(MediaStoreVolume("usb3", MediaStoreVolumeKind.REMOVABLE)),
                listOf(usb),
            ),
        )
    }

    @Test
    fun `diagnostics do not guess between multiple unmatched removable devices`() {
        val usb3 = StorageVolumeSnapshot("USB 3", null, null, "mounted", null, false, true)
        val usb4 = StorageVolumeSnapshot("USB 4", null, null, "mounted", null, false, true)

        assertTrue(
            resolveStorageVolumes(
                listOf(MediaStoreVolume("vendor_usb", MediaStoreVolumeKind.REMOVABLE)),
                listOf(usb3, usb4),
            ).isEmpty(),
        )
    }

    @Test
    fun `diagnostics ignore unmounted removable slots for sole device fallback`() {
        val mountedUsb = StorageVolumeSnapshot("USB 4", null, null, "mounted", null, false, true)
        val emptySlot = StorageVolumeSnapshot("USB 3", null, null, "unmounted", null, false, true)

        assertEquals(
            mapOf("usb3" to mountedUsb),
            resolveStorageVolumes(
                listOf(MediaStoreVolume("usb3", MediaStoreVolumeKind.REMOVABLE)),
                listOf(mountedUsb, emptySlot),
            ),
        )
    }

    @Test
    fun `direct storage fallback resolves mounted volume by vendor identifier`() {
        val storageRoot = Files.createTempDirectory("photo-storage").toFile()
        try {
            val usb = File(storageRoot, "USB3").apply { mkdirs() }
            val snapshot = StorageVolumeSnapshot("USB 4", null, "usb3", "mounted", null, false, true)

            assertEquals(
                usb.canonicalFile,
                resolveDirectStorageRoot(
                    MediaStoreVolume("usb3", MediaStoreVolumeKind.REMOVABLE),
                    snapshot,
                    storageRoot,
                ),
            )
        } finally {
            storageRoot.deleteRecursively()
        }
    }

    @Test
    fun `direct storage fallback rejects unmounted volume`() {
        val storageRoot = Files.createTempDirectory("photo-storage").toFile()
        try {
            File(storageRoot, "USB3").mkdirs()
            val snapshot = StorageVolumeSnapshot("USB 4", null, "USB3", "unmounted", null, false, true)

            assertEquals(
                null,
                resolveDirectStorageRoot(
                    MediaStoreVolume("usb3", MediaStoreVolumeKind.REMOVABLE),
                    snapshot,
                    storageRoot,
                ),
            )
        } finally {
            storageRoot.deleteRecursively()
        }
    }

    @Test
    fun `direct file validation prevents sibling path escape`() {
        val root = Files.createTempDirectory("photo-usb").toFile()
        val sibling = File(root.parentFile, "${root.name}-other").apply { mkdirs() }
        try {
            assertTrue(isFileWithinRoot(File(root, "photos/image.jpg"), root))
            assertFalse(isFileWithinRoot(File(sibling, "image.jpg"), root))
        } finally {
            root.deleteRecursively()
            sibling.deleteRecursively()
        }
    }

    @Test
    fun `direct browser reads one directory and scans subfolders only on request`() = runBlocking {
        val root = Files.createTempDirectory("photo-usb").toFile()
        try {
            val album = File(root, "Album").apply { mkdirs() }
            File(root, "cover.jpg").writeBytes(byteArrayOf(1))
            File(root, "notes.txt").writeText("not a photo")
            File(album, "nested.png").writeBytes(byteArrayOf(2))
            val source = DirectUsbPhotoSource(root.absolutePath)

            val listing = source.loadDirectory(root.absolutePath)
            assertEquals(listOf("Album"), listing.directories.map(DirectUsbDirectory::name))
            assertEquals(listOf("cover.jpg"), listing.photos.map(MediaStorePhoto::name))

            val recursivelyFound = source.loadFolderPhotos(root.absolutePath) {}
            assertEquals(setOf("cover.jpg", "nested.png"), recursivelyFound.map(MediaStorePhoto::name).toSet())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `album summaries preserve folder order and count every member`() {
        val entries = listOf(
            MediaStoreAlbumEntry("camera", "Camera"),
            MediaStoreAlbumEntry("camera", "Camera"),
            MediaStoreAlbumEntry("screenshots", "Screenshots"),
        )

        assertEquals(
            listOf(
                MediaStoreAlbum("camera", "Camera", 2),
                MediaStoreAlbum("screenshots", "Screenshots", 1),
            ),
            summarizeMediaStoreAlbums(entries.asSequence()),
        )
    }

    @Test
    fun `folder selection appends uniquely and can be removed together`() {
        val original = listOf("content://media/1", "content://media/2")
        val folder = listOf("content://media/2", "content://media/3")
        val selected = updateMediaStoreFolderSelection(original, folder, select = true)

        assertEquals(listOf("content://media/1", "content://media/2", "content://media/3"), selected)
        assertEquals(listOf("content://media/1"), updateMediaStoreFolderSelection(selected, folder, select = false))
    }
}
