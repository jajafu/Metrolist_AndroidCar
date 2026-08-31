package com.metrolist.music.photo

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
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
