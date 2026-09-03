package com.metrolist.music.playback

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class CarArtworkProviderTest {
    @Test
    fun `remote artwork becomes a local URI without fetching the image`() {
        val source = Uri.parse("https://lh3.googleusercontent.com/cover?q=a%2Fb")
        val uri = carArtworkUri("test.app", source)!!
        assertEquals("content", uri.scheme)
        assertEquals("test.app.carArtwork", uri.authority)
        assertEquals(listOf("image", source.toString()), uri.pathSegments)
        val icon = Uri.parse("android.resource://test.app/drawable/music_note")
        assertEquals(icon, carArtworkUri("test.app", icon))
        val customCover = Uri.parse("content://test.app.FileProvider/cache/playlist_cover_crop.jpg")
        assertEquals(customCover.toString(), carArtworkUri("test.app", customCover)!!.lastPathSegment)
        assertNull(carArtworkUri("test.app", Uri.parse("file:///data/private")))
        assertNull(carArtworkUri("test.app", Uri.parse("content://unrelated/private")))
    }

    @Test
    fun `provider rejects writes and unsupported sources before doing image work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildContentProvider(CarArtworkProvider::class.java).create()
        try {
            val provider = controller.get()
            val uri = carArtworkUri(context.packageName, Uri.parse("https://example.com/cover"))!!
            assertThrows(FileNotFoundException::class.java) { provider.openFile(uri, "w") }
            val unsafe = Uri.Builder().scheme("content").authority("${context.packageName}.carArtwork")
                .appendPath("image").appendPath("file:///data/private").build()
            assertThrows(FileNotFoundException::class.java) { provider.openFile(unsafe, "r") }
        } finally {
            controller.shutdown()
        }
    }

    @Test
    fun `cached cover is readable offline and old rendered covers are pruned`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = Robolectric.buildContentProvider(CarArtworkProvider::class.java).create()
        val directory = File(context.cacheDir, "car-artwork").apply { mkdirs() }
        try {
            val source = "https://example.com/offline-cover"
            val key = MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
                .joinToString("") { "%02x".format(it) }
            File(directory, "$key.png").outputStream().use {
                Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            repeat(100) { File(directory, "old-$it.png").writeBytes(byteArrayOf(0)) }
            val uri = carArtworkUri(context.packageName, Uri.parse(source))!!
            controller.get().openFile(uri, "r").use {
                val bitmap = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                assertTrue(bitmap != null)
                assertEquals(16, bitmap.width)
            }
            assertEquals(96, directory.listFiles { file -> file.extension == "png" }!!.size)
        } finally {
            directory.listFiles()?.forEach(File::delete)
            controller.shutdown()
        }
    }
}
