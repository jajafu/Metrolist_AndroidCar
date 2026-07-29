/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.metrolist.music.MainActivity
import com.metrolist.music.playback.MusicService
import timber.log.Timber

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            MusicWidgetReceiver.ACTION_PLAY_PAUSE,
            MusicWidgetReceiver.ACTION_LIKE,
            MusicWidgetReceiver.ACTION_NEXT,
            MusicWidgetReceiver.ACTION_PREVIOUS,
            -> startMusicService(context, intent.action, intent)

            TurntableWidgetReceiver.ACTION_TURNTABLE_PLAY_PAUSE ->
                startMusicService(context, MusicWidgetReceiver.ACTION_PLAY_PAUSE, intent)

            TurntableWidgetReceiver.ACTION_TURNTABLE_NEXT ->
                startMusicService(context, MusicWidgetReceiver.ACTION_NEXT, intent)

            TurntableWidgetReceiver.ACTION_TURNTABLE_PREVIOUS ->
                startMusicService(context, MusicWidgetReceiver.ACTION_PREVIOUS, intent)

            PlaylistWidgetReceiver.ACTION_PLAY_TARGET -> playPlaylistTarget(context, intent)

            MusicRecognizerWidgetReceiver.ACTION_START_RECOGNITION,
            MusicRecognizerWidgetReceiver.ACTION_UPDATE_WIDGET,
            MusicRecognizerWidgetReceiver.ACTION_RESET_STATE,
            -> MusicRecognizerWidgetReceiver().handlePrivateAction(context, intent)
        }
    }

    private fun startMusicService(
        context: Context,
        action: String?,
        source: Intent,
    ) {
        runCatching {
            val serviceIntent =
                Intent(context, MusicService::class.java).apply {
                    this.action = action
                    putExtras(source)
                }
            context.startService(WidgetActionSecurity.authenticate(serviceIntent))
        }.onFailure { error ->
            Timber.tag(TAG).w(error, "Failed to handle private widget action $action")
        }
    }

    private fun playPlaylistTarget(
        context: Context,
        source: Intent,
    ) {
        runCatching {
            val serviceIntent =
                Intent(context, MusicService::class.java).apply {
                    action = PlaylistWidgetReceiver.ACTION_PLAY_TARGET
                    putExtra(
                        PlaylistWidgetReceiver.EXTRA_TARGET_TYPE,
                        source.getStringExtra(PlaylistWidgetReceiver.EXTRA_TARGET_TYPE),
                    )
                    putExtra(
                        PlaylistWidgetReceiver.EXTRA_TARGET_ID,
                        source.getStringExtra(PlaylistWidgetReceiver.EXTRA_TARGET_ID),
                    )
                    putExtra(
                        PlaylistWidgetReceiver.EXTRA_TARGET_TITLE,
                        source.getStringExtra(PlaylistWidgetReceiver.EXTRA_TARGET_TITLE),
                    )
                }
            context.startService(WidgetActionSecurity.authenticate(serviceIntent))
        }.onFailure { error ->
            Timber.tag(TAG).w(error, "Failed to start playlist widget target")
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_OPEN_WIDGET_TARGET
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(
                        MainActivity.EXTRA_WIDGET_TARGET_TYPE,
                        source.getStringExtra(PlaylistWidgetReceiver.EXTRA_TARGET_TYPE),
                    )
                    putExtra(
                        MainActivity.EXTRA_WIDGET_TARGET_ID,
                        source.getStringExtra(PlaylistWidgetReceiver.EXTRA_TARGET_ID),
                    )
                },
            )
        }
    }

    private companion object {
        const val TAG = "WidgetActionReceiver"
    }
}
