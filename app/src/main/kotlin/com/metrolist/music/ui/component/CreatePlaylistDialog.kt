/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val ytmSyncEnabled by rememberPreference(YtmSyncKey, true)
    val isSignedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    var syncedPlaylist by rememberSaveable(isSignedIn, ytmSyncEnabled, allowSyncing) {
        mutableStateOf(allowSyncing && isSignedIn && ytmSyncEnabled)
    }
    var isCreating by rememberSaveable { mutableStateOf(false) }
    var pendingRemotePlaylistId by rememberSaveable { mutableStateOf<String?>(null) }

    val notLoggedInYoutubeStr = stringResource(R.string.not_logged_in_youtube)
    val syncDisabledStr = stringResource(R.string.sync_disabled)
    val createFailedStr = stringResource(R.string.playlist_create_failed)

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        autoDismiss = false,
        isInputValid = { playlistName -> playlistName.isNotBlank() && !isCreating },
        onDone = onDone@{ playlistName ->
            if (playlistName.isBlank() || isCreating) return@onDone
            isCreating = true
            coroutineScope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val browseId =
                        if (syncedPlaylist && isSignedIn) {
                            pendingRemotePlaylistId
                                ?: YouTube.createPlaylist(playlistName)
                                    .getOrThrow()
                                    .also { pendingRemotePlaylistId = it }
                        } else if (syncedPlaylist) {
                            error(notLoggedInYoutubeStr)
                        } else {
                            null
                        }

                    val playlistEntity =
                        PlaylistEntity(
                            name = playlistName,
                            browseId = browseId,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true,
                        )

                    database.withTransaction {
                        insert(playlistEntity)
                    }
                    playlistEntity.id
                }

                withContext(Dispatchers.Main) {
                    result
                        .onSuccess { playlistId ->
                            onPlaylistCreated?.invoke(playlistId)
                            onDismiss()
                        }.onFailure {
                            isCreating = false
                            Toast.makeText(context, createFailedStr, Toast.LENGTH_LONG).show()
                        }
                }
            }
        },
        extraContent = {
            if (allowSyncing) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 40.dp),
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sync_playlist),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.allows_for_sync_witch_youtube),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(0.7f),
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Switch(
                            checked = syncedPlaylist,
                            enabled = !isCreating,
                            onCheckedChange = { shouldSync ->
                                if (shouldSync && !isSignedIn) {
                                    Toast
                                        .makeText(
                                            context,
                                            notLoggedInYoutubeStr,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                } else if (shouldSync && !ytmSyncEnabled) {
                                    Toast
                                        .makeText(
                                            context,
                                            syncDisabledStr,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                } else {
                                    syncedPlaylist = shouldSync
                                }
                            },
                        )
                    }
                }
            }
        },
    )
}
