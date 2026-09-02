package com.metrolist.music.photo

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

internal data class MediaStoreVolume(
    val name: String,
    val kind: MediaStoreVolumeKind,
)

internal enum class MediaStoreVolumeKind { ALL, PRIMARY, REMOVABLE, LEGACY }

internal data class MediaStorePhoto(
    val uri: String,
    val name: String,
)

internal data class MediaStoreAlbum(
    val id: String,
    val name: String,
    val photoCount: Int,
)

internal data class MediaStoreAlbumEntry(
    val albumId: String,
    val albumName: String,
)

internal data class MediaStorePhotoPage(
    val photos: List<MediaStorePhoto>,
    val nextOffset: Int,
    val hasMore: Boolean,
)

internal data class StorageVolumeSnapshot(
    val description: String,
    val mediaStoreName: String?,
    val uuid: String?,
    val state: String,
    val path: String?,
    val primary: Boolean,
    val removable: Boolean,
)

internal data class MediaStoreVolumeDiagnostic(
    val volume: MediaStoreVolume,
    val indexedPhotoCount: Int?,
    val storage: StorageVolumeSnapshot?,
)

internal class MediaStorePhotoSource(private val context: Context) {
    private val resolver = context.contentResolver

    suspend fun availableVolumes(): List<MediaStoreVolume> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext listOf(MediaStoreVolume("external", MediaStoreVolumeKind.LEGACY))
        }

        val attached = MediaStore.getExternalVolumeNames(context)
        buildList {
            add(MediaStoreVolume(MediaStore.VOLUME_EXTERNAL, MediaStoreVolumeKind.ALL))
            if (MediaStore.VOLUME_EXTERNAL_PRIMARY in attached) {
                add(MediaStoreVolume(MediaStore.VOLUME_EXTERNAL_PRIMARY, MediaStoreVolumeKind.PRIMARY))
            }
            attached.asSequence()
                .filterNot { it == MediaStore.VOLUME_EXTERNAL_PRIMARY }
                .sorted()
                .forEach { add(MediaStoreVolume(it, MediaStoreVolumeKind.REMOVABLE)) }
        }
    }

    suspend fun loadAlbums(volume: MediaStoreVolume): List<MediaStoreAlbum> = withContext(Dispatchers.IO) {
        val collection = collection(volume)
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val signal = CancellationSignal()
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { signal.cancel() }
        try {
            val cursor = requireNotNull(
                resolver.query(
                    collection,
                    projection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC",
                    signal,
                ),
            )
            cursor.use {
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val albumNameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                summarizeMediaStoreAlbums(
                    sequence {
                        while (it.moveToNext()) {
                            signal.throwIfCanceled()
                            val albumId = it.getString(albumIdColumn)?.takeIf(String::isNotBlank) ?: continue
                            yield(
                                MediaStoreAlbumEntry(
                                    albumId = albumId,
                                    albumName = it.getString(albumNameColumn).orEmpty(),
                                ),
                            )
                        }
                    },
                )
            }
        } finally {
            cancellation.dispose()
        }
    }

    suspend fun loadPage(
        volume: MediaStoreVolume,
        offset: Int,
        limit: Int,
        albumId: String? = null,
    ): MediaStorePhotoPage = withContext(Dispatchers.IO) {
        require(offset >= 0)
        require(limit > 0)
        val collection = collection(volume)
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val signal = CancellationSignal()
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { signal.cancel() }
        try {
            val result = queryPage(collection, projection, albumId, offset, limit + 1, signal)
            result.cursor.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                if (result.rowsToSkip > 0) cursor.moveToPosition(result.rowsToSkip - 1)
                val photos = ArrayList<MediaStorePhoto>(limit + 1)
                while (photos.size <= limit && cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    photos += MediaStorePhoto(
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        name = cursor.getString(nameColumn)?.takeIf(String::isNotBlank) ?: id.toString(),
                    )
                }
                val hasMore = photos.size > limit
                val visible = if (hasMore) photos.subList(0, limit).toList() else photos
                MediaStorePhotoPage(
                    photos = visible,
                    nextOffset = offset + visible.size,
                    hasMore = hasMore,
                )
            }
        } finally {
            cancellation.dispose()
        }
    }

    suspend fun loadAlbumPhotos(volume: MediaStoreVolume, albumId: String): List<MediaStorePhoto> = withContext(Dispatchers.IO) {
        val collection = collection(volume)
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val signal = CancellationSignal()
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { signal.cancel() }
        try {
            val cursor = requireNotNull(
                resolver.query(
                    collection,
                    projection,
                    "${MediaStore.Images.Media.BUCKET_ID} = ?",
                    arrayOf(albumId),
                    "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC",
                    signal,
                ),
            )
            cursor.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                buildList(it.count) {
                    while (it.moveToNext()) {
                        signal.throwIfCanceled()
                        val id = it.getLong(idColumn)
                        add(
                            MediaStorePhoto(
                                uri = ContentUris.withAppendedId(collection, id).toString(),
                                name = it.getString(nameColumn)?.takeIf(String::isNotBlank) ?: id.toString(),
                            ),
                        )
                    }
                }
            }
        } finally {
            cancellation.dispose()
        }
    }

    suspend fun loadVolumeDiagnostics(volumes: List<MediaStoreVolume>): List<MediaStoreVolumeDiagnostic> = withContext(Dispatchers.IO) {
        val storageVolumes = storageVolumeSnapshots(context)
        val resolvedStorage = resolveStorageVolumes(volumes, storageVolumes)
        volumes.map { volume ->
            val count = try {
                countImages(volume)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                null
            }
            MediaStoreVolumeDiagnostic(
                volume = volume,
                indexedPhotoCount = count,
                storage = resolvedStorage[volume.name]?.let { storage ->
                    storage.copy(path = storage.path ?: resolveDirectStorageRoot(volume, storage)?.absolutePath)
                },
            )
        }
    }

    suspend fun directRemovableRoot(volume: MediaStoreVolume): DirectUsbRoot? = withContext(Dispatchers.IO) {
        if (volume.kind != MediaStoreVolumeKind.REMOVABLE) return@withContext null
        val storage = resolveStorageVolumes(listOf(volume), storageVolumeSnapshots(context))[volume.name]
            ?: return@withContext null
        val directory = resolveDirectStorageRoot(volume, storage) ?: return@withContext null
        DirectUsbRoot(storage.description, directory.absolutePath)
    }

    private suspend fun countImages(volume: MediaStoreVolume): Int {
        val signal = CancellationSignal()
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { signal.cancel() }
        return try {
            requireNotNull(
                resolver.query(
                    collection(volume),
                    arrayOf(MediaStore.Images.Media._ID),
                    null,
                    null,
                    null,
                    signal,
                ),
            ).use { it.count }
        } finally {
            cancellation.dispose()
        }
    }

    private fun collection(volume: MediaStoreVolume): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(volume.name)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    private fun queryPage(
        collection: Uri,
        projection: Array<String>,
        albumId: String?,
        offset: Int,
        limit: Int,
        signal: CancellationSignal,
    ): PageCursor {
        val queryArgs = Bundle().apply {
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media._ID),
            )
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            if (albumId != null) {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.BUCKET_ID} = ?")
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(albumId))
            }
        }
        return try {
            val cursor = requireNotNull(resolver.query(collection, projection, queryArgs, signal))
            // A few vendor providers accept query arguments but ignore pagination.
            PageCursor(cursor, rowsToSkip = if (cursor.count > limit) offset else 0)
        } catch (_: IllegalArgumentException) {
            val cursor = requireNotNull(
                resolver.query(
                    collection,
                    projection,
                    albumId?.let { "${MediaStore.Images.Media.BUCKET_ID} = ?" },
                    if (albumId != null) arrayOf(albumId) else null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC",
                    signal,
                ),
            )
            PageCursor(cursor, rowsToSkip = offset)
        }
    }

    private data class PageCursor(val cursor: Cursor, val rowsToSkip: Int)
}

internal fun storageVolumeSnapshots(context: Context): List<StorageVolumeSnapshot> {
    val storageManager = context.getSystemService(StorageManager::class.java)
    return storageManager.storageVolumes.map { volume ->
        StorageVolumeSnapshot(
            description = volume.getDescription(context),
            mediaStoreName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) volume.mediaStoreVolumeName else null,
            uuid = volume.uuid,
            state = volume.state,
            path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) volume.directory?.absolutePath else null,
            primary = volume.isPrimary,
            removable = volume.isRemovable,
        )
    }
}

internal fun resolveDirectStorageRoot(
    volume: MediaStoreVolume,
    storage: StorageVolumeSnapshot,
    storageDirectory: File = File("/storage"),
): File? {
    if (!storage.removable || storage.primary ||
        (storage.state != Environment.MEDIA_MOUNTED && storage.state != Environment.MEDIA_MOUNTED_READ_ONLY)
    ) return null

    val base = runCatching { storageDirectory.canonicalFile }.getOrNull() ?: return null
    val systemPath = storage.path?.let(::File)
    val identifiers = listOfNotNull(storage.uuid, storage.mediaStoreName, volume.name)
        .mapNotNull(::normalizedVolumeId)
        .toSet()
    val candidates = buildList {
        systemPath?.let(::add)
        storageDirectory.listFiles()?.forEach(::add)
    }
    return candidates.asSequence()
        .mapNotNull { runCatching { it.canonicalFile }.getOrNull() }
        .filter { it.parentFile == base && it.isDirectory && it.canRead() }
        .firstOrNull { candidate ->
            candidate == systemPath?.let { runCatching { it.canonicalFile }.getOrNull() } ||
                normalizedVolumeId(candidate.name) in identifiers
        }
}

internal fun mountedDirectStorageRoots(context: Context): List<File> = storageVolumeSnapshots(context)
    .asSequence()
    .filter { it.removable && !it.primary }
    .mapNotNull { storage ->
        val name = storage.mediaStoreName ?: storage.uuid ?: return@mapNotNull null
        resolveDirectStorageRoot(MediaStoreVolume(name, MediaStoreVolumeKind.REMOVABLE), storage)
    }
    .distinctBy { it.absolutePath }
    .toList()

@SuppressLint("InlinedApi")
internal fun requiredMediaStorePhotoPermissions(sdk: Int = Build.VERSION.SDK_INT): Array<String> = when {
    sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    sdk >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@SuppressLint("InlinedApi")
internal fun hasMediaStorePhotoAccess(
    context: Context,
    sdk: Int = Build.VERSION.SDK_INT,
    permissionCheck: (String) -> Int = { ContextCompat.checkSelfPermission(context, it) },
): Boolean {
    val granted = PackageManager.PERMISSION_GRANTED
    return when {
        sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            permissionCheck(Manifest.permission.READ_MEDIA_IMAGES) == granted ||
                permissionCheck(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == granted
        sdk >= Build.VERSION_CODES.TIRAMISU -> permissionCheck(Manifest.permission.READ_MEDIA_IMAGES) == granted
        else -> permissionCheck(Manifest.permission.READ_EXTERNAL_STORAGE) == granted
    }
}

internal fun mergeMediaStorePhotos(
    current: List<MediaStorePhoto>,
    page: List<MediaStorePhoto>,
): List<MediaStorePhoto> = (current + page).distinctBy(MediaStorePhoto::uri)

internal fun resolveStorageVolumes(
    mediaVolumes: List<MediaStoreVolume>,
    storageVolumes: List<StorageVolumeSnapshot>,
): Map<String, StorageVolumeSnapshot> {
    val resolved = linkedMapOf<String, StorageVolumeSnapshot>()
    mediaVolumes.firstOrNull { it.kind == MediaStoreVolumeKind.PRIMARY }?.let { primary ->
        storageVolumes.firstOrNull(StorageVolumeSnapshot::primary)?.let { resolved[primary.name] = it }
    }

    val removableMedia = mediaVolumes.filter { it.kind == MediaStoreVolumeKind.REMOVABLE }
    val removableStorage = storageVolumes.filter {
        it.removable && !it.primary &&
            (it.state == Environment.MEDIA_MOUNTED || it.state == Environment.MEDIA_MOUNTED_READ_ONLY)
    }
    removableMedia.forEach { media ->
        removableStorage.firstOrNull { storage ->
            storage.mediaStoreName.equals(media.name, ignoreCase = true) ||
                normalizedVolumeId(storage.uuid) == normalizedVolumeId(media.name)
        }?.let { resolved[media.name] = it }
    }

    val unmatchedMedia = removableMedia.filterNot { it.name in resolved }
    val matchedStorage = resolved.values.toSet()
    val unmatchedStorage = removableStorage.filterNot { it in matchedStorage }
    if (unmatchedMedia.size == 1 && unmatchedStorage.size == 1) {
        resolved[unmatchedMedia.single().name] = unmatchedStorage.single()
    }
    return resolved
}

internal fun normalizedVolumeId(value: String?): String? = value
    ?.lowercase(Locale.ROOT)
    ?.replace("-", "")
    ?.takeIf(String::isNotBlank)

internal fun summarizeMediaStoreAlbums(entries: Sequence<MediaStoreAlbumEntry>): List<MediaStoreAlbum> {
    data class MutableAlbum(val id: String, val name: String, var count: Int)

    val albums = linkedMapOf<String, MutableAlbum>()
    entries.forEach { entry ->
        val album = albums[entry.albumId]
        if (album == null) {
            albums[entry.albumId] = MutableAlbum(entry.albumId, entry.albumName, 1)
        } else {
            album.count++
        }
    }
    return albums.values.map { MediaStoreAlbum(it.id, it.name, it.count) }
}

internal fun updateMediaStoreFolderSelection(
    current: List<String>,
    folderUris: List<String>,
    select: Boolean,
): List<String> = if (select) {
    (current + folderUris).distinct()
} else {
    val removed = folderUris.toHashSet()
    current.filterNot(removed::contains)
}
