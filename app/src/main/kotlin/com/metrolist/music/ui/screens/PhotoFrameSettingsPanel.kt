package com.metrolist.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.photo.FrameCatalogState
import com.metrolist.music.photo.FrameError
import com.metrolist.music.photo.FrameSelectionType
import com.metrolist.music.photo.FrameSettings
import com.metrolist.music.photo.FrameSource
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoFrameSettingsPanel(
    state: FrameCatalogState,
    busy: Boolean,
    error: FrameError?,
    onDismiss: () -> Unit,
    onBrowsePhotos: () -> Unit,
    onPickPhotos: () -> Unit,
    onPickFolder: (FrameSource?) -> Unit,
    onRescan: () -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onCancelScan: () -> Unit,
    onSettings: (FrameSettings) -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val enabled = state.initialized && !busy
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.photo_frame), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.photo_frame_local_only), style = MaterialTheme.typography.bodyMedium)
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onBrowsePhotos, enabled = enabled) {
                        Text(stringResource(R.string.photo_browser_browse_device))
                    }
                    OutlinedButton(onClick = onPickPhotos, enabled = enabled) {
                        Text(stringResource(R.string.photo_browser_system_picker))
                    }
                    OutlinedButton(onClick = { onPickFolder(null) }, enabled = enabled) {
                        Text(stringResource(R.string.photo_frame_pick_folder))
                    }
                }
                if (state.sources.isEmpty()) Text(stringResource(R.string.photo_frame_empty))
            }
            if (busy) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.photo_frame_scanning, state.scanCount))
                    TextButton(onClick = onCancelScan) { Text(stringResource(R.string.photo_frame_cancel)) }
                }
            }
            if (error != null) item {
                Text(stringResource(frameErrorMessage(error)), color = MaterialTheme.colorScheme.error)
            }
            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.photo_frame_display),
                    items = listOf(
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.photo_frame_interval)) },
                            trailingContent = { Text(stringResource(R.string.photo_frame_seconds, state.settings.intervalSeconds)) },
                            enabled = enabled,
                            onClick = {
                                val intervals = listOf(5, 10, 15, 30, 60)
                                val next = (intervals.indexOf(state.settings.intervalSeconds) + 1) % intervals.size
                                onSettings(state.settings.copy(intervalSeconds = intervals[next]))
                            },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.photo_frame_fill)) },
                            description = { Text(stringResource(R.string.photo_frame_fill_desc)) },
                            trailingContent = {
                                FrameSettingSwitch(R.string.photo_frame_fill, state.settings.crop, enabled) { onSettings(state.settings.copy(crop = it)) }
                            },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.photo_frame_clock)) },
                            trailingContent = {
                                FrameSettingSwitch(R.string.photo_frame_clock, state.settings.showClock, enabled) { onSettings(state.settings.copy(showClock = it)) }
                            },
                        ),
                        Material3SettingsItem(
                            title = { Text(stringResource(R.string.photo_frame_song_info)) },
                            trailingContent = {
                                FrameSettingSwitch(R.string.photo_frame_song_info, state.settings.showSongInfo, enabled) { onSettings(state.settings.copy(showSongInfo = it)) }
                            },
                        ),
                    ),
                )
            }
            item {
                Text(stringResource(R.string.photo_frame_sources, state.photos.size), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRescan, enabled = enabled && state.sources.isNotEmpty()) {
                        Text(stringResource(R.string.photo_frame_rescan))
                    }
                    TextButton(onClick = { confirmClear = true }, enabled = enabled && state.sources.isNotEmpty()) {
                        Text(stringResource(R.string.photo_frame_clear))
                    }
                }
            }
            items(state.sources, key = { "${it.type}:${it.uri}" }) { source ->
                ListItem(
                    headlineContent = { Text(source.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    overlineContent = {
                        Text(stringResource(if (source.type == FrameSelectionType.FOLDER) R.string.photo_frame_folder else R.string.photo_frame_photo))
                    },
                    supportingContent = {
                        Text(
                            when {
                                source.needsPermission -> stringResource(R.string.photo_frame_permission)
                                source.unavailable -> stringResource(R.string.photo_frame_unreadable)
                                source.type == FrameSelectionType.FOLDER && !source.scanned -> stringResource(R.string.photo_frame_not_scanned)
                                source.unreadableCount > 0 -> stringResource(R.string.photo_frame_failed_photos, source.unreadableCount, source.photoCount)
                                source.photoCount == 0 -> stringResource(R.string.photo_frame_no_images)
                                else -> stringResource(R.string.photo_frame_sources, source.photoCount)
                            },
                        )
                    },
                    trailingContent = {
                        Row {
                            if (source.needsPermission || source.unavailable) {
                                IconButton(
                                    onClick = { if (source.type == FrameSelectionType.FOLDER) onPickFolder(source) else onPickPhotos() },
                                    enabled = enabled,
                                ) {
                                    Icon(painterResource(R.drawable.refresh), stringResource(R.string.photo_frame_reauthorize))
                                }
                            }
                            IconButton(onClick = { onRemove(source.uri) }, enabled = enabled) {
                                Icon(painterResource(R.drawable.close), stringResource(R.string.photo_frame_remove, source.name))
                            }
                        }
                    },
                )
            }
            item {
                TextButton(onClick = onDismiss, modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(stringResource(R.string.photo_frame_done))
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.photo_frame_clear)) },
            text = { Text(stringResource(R.string.photo_frame_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClear() }, enabled = enabled) {
                    Text(stringResource(R.string.photo_frame_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.photo_frame_cancel)) }
            },
        )
    }
}

@Composable
private fun FrameSettingSwitch(label: Int, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val description = stringResource(label)
    Switch(checked, onCheckedChange, Modifier.semantics { contentDescription = description }, enabled = enabled)
}

internal fun frameErrorMessage(error: FrameError): Int = when (error) {
    FrameError.STORAGE -> R.string.photo_frame_storage_error
    FrameError.PERMISSION -> R.string.photo_frame_permission
    FrameError.UNREADABLE -> R.string.photo_frame_unreadable
    FrameError.INVALID_IMAGE -> R.string.photo_frame_invalid_image
    FrameError.MANIFEST -> R.string.photo_frame_manifest_error
    FrameError.PICKER_UNAVAILABLE -> R.string.photo_frame_picker_unavailable
}
