package com.metrolist.music.utils

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CoilBitmapLoaderTest {
    @Test
    fun `media bitmap copy remains valid after source is recycled`() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.RGB_565)
        source.setPixel(0, 0, Color.RED)

        val copy = source.createIndependentSoftwareCopy()
        source.recycle()

        assertNotSame(source, copy)
        assertEquals(Bitmap.Config.ARGB_8888, copy.config)
        assertFalse(copy.isMutable)
        assertFalse(copy.isRecycled)
        assertEquals(Color.RED, copy.getPixel(0, 0))
    }

    @Test
    fun `recycled source returns a safe software fallback`() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        source.recycle()

        val fallback = source.createIndependentSoftwareCopy()

        assertEquals(64, fallback.width)
        assertEquals(64, fallback.height)
        assertEquals(Bitmap.Config.ARGB_8888, fallback.config)
        assertFalse(fallback.isRecycled)
    }
}
