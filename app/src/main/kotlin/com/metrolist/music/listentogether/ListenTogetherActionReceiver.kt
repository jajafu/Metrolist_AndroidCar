package com.metrolist.music.listentogether

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class ListenTogetherActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var managerProvider: Provider<ListenTogetherManager>

    override fun onReceive(context: Context, intent: Intent) {
        val manager = managerProvider.get()
        manager.initialize()
        val notifId = intent.getIntExtra(ListenTogetherClient.EXTRA_NOTIFICATION_ID, 0)

        // Cancel the notification immediately
        NotificationManagerCompat.from(context).cancel(notifId)

        when (intent.action) {
            ListenTogetherClient.ACTION_APPROVE_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                scope.launch {
                    manager.approveJoin(userId)
                }
            }
            ListenTogetherClient.ACTION_REJECT_JOIN -> {
                val userId = intent.getStringExtra(ListenTogetherClient.EXTRA_USER_ID) ?: return
                scope.launch {
                    manager.rejectJoin(userId, null)
                }
            }
            ListenTogetherClient.ACTION_APPROVE_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                scope.launch {
                    manager.approveSuggestion(suggestionId)
                }
            }
            ListenTogetherClient.ACTION_REJECT_SUGGESTION -> {
                val suggestionId = intent.getStringExtra(ListenTogetherClient.EXTRA_SUGGESTION_ID) ?: return
                scope.launch {
                    manager.rejectSuggestion(suggestionId, null)
                }
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
