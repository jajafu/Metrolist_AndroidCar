/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundServiceStartExceptionCompatTest {
    private val exceptionClassName =
        "android.app.ForegroundServiceStartNotAllowedException"

    @Test
    fun `API 31 recognizes foreground service start denial`() {
        assertTrue(
            ForegroundServiceStartExceptionCompat.isStartNotAllowed(
                sdkInt = 31,
                exceptionClassName = exceptionClassName,
            ),
        )
    }

    @Test
    fun `API 30 does not inspect API 31 exception`() {
        assertFalse(
            ForegroundServiceStartExceptionCompat.isStartNotAllowed(
                sdkInt = 30,
                exceptionClassName = exceptionClassName,
            ),
        )
    }

    @Test
    fun `unrelated illegal state exception is not treated as start denial`() {
        assertFalse(
            ForegroundServiceStartExceptionCompat.isStartNotAllowed(
                sdkInt = 31,
                exceptionClassName = IllegalStateException::class.java.name,
            ),
        )
    }
}
