package com.metrolist.music.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import com.metrolist.music.R
import com.metrolist.music.photo.MediaStorePhoto
import com.metrolist.music.photo.MediaStorePhotoSource
import com.metrolist.music.photo.MediaStoreVolume
import com.metrolist.music.photo.MediaStoreVolumeKind
import com.metrolist.music.photo.hasMediaStorePhotoAccess
import com.metrolist.music.photo.mergeMediaStorePhotos
import com.metrolist.music.photo.requiredMediaStorePhotoPermissions
import kotlinx.coroutines.CancellationException

private const val MediaStorePageSize = 80

@Composable
internal fun MediaStorePhotoBrowser(
    onDismiss: () -> Unit,
    onPhotosSelected: (List<Uri>) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val source = remember(context) { MediaStorePhotoSource(context.applicationContext) }
    val selected = remember { mutableStateListOf<String>() }
    var permissionRevision by remember { mutableIntStateOf(0) }
    val hasAccess = remember(permissionRevision) { hasMediaStorePhotoAccess(context) }
    var volumes by remember { mutableStateOf(emptyList<MediaStoreVolume>()) }
    var selectedVolume by remember { mutableStateOf<MediaStoreVolume?>(null) }
    var photos by remember { mutableStateOf(emptyList<MediaStorePhoto>()) }
    var nextOffset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var requestedOffset by remember { mutableIntStateOf(0) }
    var requestRevision by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionRevision++
    }

    BackHandler(onBack = onDismiss)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRevision++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasAccess, permissionRevision) {
        if (!hasAccess) {
            volumes = emptyList()
            selectedVolume = null
            photos = emptyList()
            return@LaunchedEffect
        }
        try {
            val available = source.availableVolumes()
            volumes = available
            val retained = available.firstOrNull { it.name == selectedVolume?.name }
            selectedVolume = retained ?: available.firstOrNull()
            requestedOffset = 0
            requestRevision++
            loadError = false
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            volumes = emptyList()
            selectedVolume = null
            loadError = true
        }
    }

    LaunchedEffect(selectedVolume, requestRevision) {
        val volume = selectedVolume ?: return@LaunchedEffect
        val offset = requestedOffset
        loading = true
        loadError = false
        try {
            val page = source.loadPage(volume, offset, MediaStorePageSize)
            photos = if (offset == 0) page.photos else mergeMediaStorePhotos(photos, page.photos)
            nextOffset = page.nextOffset
            hasMore = page.hasMore
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            loadError = true
        } finally {
            loading = false
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            MediaStoreBrowserTopBar(
                selectedCount = selected.size,
                onDismiss = onDismiss,
                onAdd = {
                    onPhotosSelected(selected.distinct().map(Uri::parse))
                },
            )
            when {
                !hasAccess -> MediaStorePermissionContent(
                    onRequest = { permissionLauncher.launch(requiredMediaStorePhotoPermissions()) },
                    onSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                )
                else -> {
                    if (volumes.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowItems(volumes, key = MediaStoreVolume::name) { volume ->
                                FilterChip(
                                    selected = volume.name == selectedVolume?.name,
                                    onClick = {
                                        if (volume.name != selectedVolume?.name) {
                                            selectedVolume = volume
                                            photos = emptyList()
                                            requestedOffset = 0
                                            requestRevision++
                                        }
                                    },
                                    label = { Text(mediaStoreVolumeLabel(volume)) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                    MediaStorePhotoGrid(
                        photos = photos,
                        selected = selected,
                        loading = loading,
                        loadError = loadError,
                        hasMore = hasMore,
                        onRetry = { requestRevision++ },
                        onLoadMore = {
                            requestedOffset = nextOffset
                            requestRevision++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaStoreBrowserTopBar(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.photo_browser_close))
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.photo_browser_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selectedCount > 0) {
                Text(
                    pluralStringResource(R.plurals.photo_browser_selected, selectedCount, selectedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(onClick = onAdd, enabled = selectedCount > 0, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.photo_browser_add, selectedCount))
        }
    }
}

@Composable
private fun MediaStorePermissionContent(onRequest: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(R.drawable.insert_photo), null, Modifier.size(56.dp))
        Spacer(Modifier.size(16.dp))
        Text(
            stringResource(R.string.photo_browser_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.photo_browser_permission_description),
            Modifier.widthIn(max = 560.dp).padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FlowRow(
            Modifier.widthIn(max = 560.dp).padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onRequest, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.photo_browser_allow))
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.photo_browser_app_settings))
            }
        }
    }
}

@Composable
private fun MediaStorePhotoGrid(
    photos: List<MediaStorePhoto>,
    selected: MutableList<String>,
    loading: Boolean,
    loadError: Boolean,
    hasMore: Boolean,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when {
        photos.isEmpty() && loading -> BrowserCenteredMessage {
            CircularProgressIndicator()
            Text(stringResource(R.string.photo_browser_loading), Modifier.padding(top = 12.dp))
        }
        photos.isEmpty() && loadError -> BrowserCenteredMessage {
            Text(stringResource(R.string.photo_browser_load_error), color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
        photos.isEmpty() -> BrowserCenteredMessage {
            Icon(painterResource(R.drawable.hide_image), null, Modifier.size(48.dp))
            Text(stringResource(R.string.photo_browser_empty), Modifier.padding(top = 12.dp))
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 132.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItems(photos, key = MediaStorePhoto::uri) { photo ->
                MediaStorePhotoTile(
                    photo = photo,
                    selected = photo.uri in selected,
                    onToggle = {
                        if (photo.uri in selected) selected.remove(photo.uri) else selected.add(photo.uri)
                    },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    when {
                        loading -> CircularProgressIndicator(Modifier.size(32.dp))
                        loadError -> Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.retry))
                        }
                        hasMore -> OutlinedButton(onClick = onLoadMore, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.photo_browser_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaStorePhotoTile(photo: MediaStorePhoto, selected: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val description = stringResource(R.string.photo_browser_photo_description, photo.name)
    val request = remember(photo.uri) {
        ImageRequest.Builder(context)
            .data(photo.uri.toUri())
            .size(320, 320)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .build()
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.Checkbox, onClick = onToggle)
            .semantics { contentDescription = description },
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.hide_image),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                .padding(start = 8.dp, top = 20.dp, end = 40.dp, bottom = 8.dp),
        ) {
            Text(photo.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp),
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (selected) Icon(painterResource(R.drawable.check), null, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun BrowserCenteredMessage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun mediaStoreVolumeLabel(volume: MediaStoreVolume): String = when (volume.kind) {
    MediaStoreVolumeKind.ALL -> stringResource(R.string.photo_browser_all_storage)
    MediaStoreVolumeKind.PRIMARY -> stringResource(R.string.photo_browser_internal_storage)
    MediaStoreVolumeKind.REMOVABLE -> stringResource(R.string.photo_browser_external_storage, volume.name.take(8))
    MediaStoreVolumeKind.LEGACY -> stringResource(R.string.photo_browser_device_storage)
}
