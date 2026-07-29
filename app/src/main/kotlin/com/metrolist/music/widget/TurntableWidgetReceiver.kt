/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.metrolist.music.playback.MusicService

class TurntableWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Only trigger update through MusicService if it's already running
        // This prevents BackgroundServiceStartNotAllowedException on Android 14+
        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_TURNTABLE_WIDGET
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Service might be restricted in background
            }
        }
        // If service is not running, widget shows default layout until user opens app
    }

    companion object {
        const val ACTION_TURNTABLE_PLAY_PAUSE = "com.metrolist.music.widget.TURNTABLE_PLAY_PAUSE"
        const val ACTION_TURNTABLE_NEXT = "com.metrolist.music.widget.TURNTABLE_NEXT"
        const val ACTION_TURNTABLE_PREVIOUS = "com.metrolist.music.widget.TURNTABLE_PREVIOUS"
        const val ACTION_UPDATE_TURNTABLE_WIDGET = "com.metrolist.music.widget.UPDATE_TURNTABLE_WIDGET"
    }
}
