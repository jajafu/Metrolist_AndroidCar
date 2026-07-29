/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.widget

import android.content.Intent
import java.util.UUID

internal object WidgetActionSecurity {
    private const val EXTRA_ACTION_TOKEN = "com.metrolist.music.widget.extra.ACTION_TOKEN"
    private val actionToken = UUID.randomUUID().toString()

    fun authenticate(intent: Intent): Intent =
        intent.apply {
            putExtra(EXTRA_ACTION_TOKEN, actionToken)
        }

    fun isAuthenticated(intent: Intent): Boolean =
        intent.getStringExtra(EXTRA_ACTION_TOKEN) == actionToken

    fun requiresAuthentication(action: String?): Boolean =
        action == MusicWidgetReceiver.ACTION_PLAY_PAUSE ||
            action == MusicWidgetReceiver.ACTION_LIKE ||
            action == MusicWidgetReceiver.ACTION_NEXT ||
            action == MusicWidgetReceiver.ACTION_PREVIOUS ||
            action == PlaylistWidgetReceiver.ACTION_PLAY_TARGET
}
