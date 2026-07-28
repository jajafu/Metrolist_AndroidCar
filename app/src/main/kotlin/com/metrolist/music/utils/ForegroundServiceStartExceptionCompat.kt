/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.os.Build

internal object ForegroundServiceStartExceptionCompat {
    private const val START_NOT_ALLOWED_EXCEPTION_CLASS =
        "android.app.ForegroundServiceStartNotAllowedException"

    fun isStartNotAllowed(error: Throwable): Boolean =
        isStartNotAllowed(
            sdkInt = Build.VERSION.SDK_INT,
            exceptionClassName = error.javaClass.name,
        )

    internal fun isStartNotAllowed(
        sdkInt: Int,
        exceptionClassName: String,
    ): Boolean =
        sdkInt >= Build.VERSION_CODES.S &&
            exceptionClassName == START_NOT_ALLOWED_EXCEPTION_CLASS
}
