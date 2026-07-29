/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class PlaylistWidgetReceiver : AppWidgetProvider() {
    @Inject
    lateinit var playlistWidgetManager: PlaylistWidgetManager

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refreshIdleWidgets(appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refreshIdleWidget(appWidgetId, newOptions)
    }

    private fun refreshIdleWidgets(
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                appWidgetIds.forEach { appWidgetId ->
                    playlistWidgetManager.updateIdleWidget(
                        appWidgetId = appWidgetId,
                        options = appWidgetManager.getAppWidgetOptions(appWidgetId),
                    )
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to refresh playlist widgets")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun refreshIdleWidget(appWidgetId: Int, options: android.os.Bundle) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                playlistWidgetManager.updateIdleWidget(appWidgetId, options)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to refresh playlist widget")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PlaylistWidgetReceiver"
        const val ACTION_PLAY_TARGET = "com.metrolist.music.widget.playlists.PLAY_TARGET"
        const val ACTION_UPDATE_WIDGET = "com.metrolist.music.widget.playlists.UPDATE_WIDGET"

        const val EXTRA_TARGET_TYPE = "playlist_widget_target_type"
        const val EXTRA_TARGET_ID = "playlist_widget_target_id"
        const val EXTRA_TARGET_TITLE = "playlist_widget_target_title"

        const val TARGET_TYPE_LOCAL = "local"
        const val TARGET_TYPE_ONLINE = "online"
        const val TARGET_TYPE_LIKED = "liked"
        const val TARGET_TYPE_DOWNLOADED = "downloaded"
        const val TARGET_TYPE_TOP = "top"
    }
}
