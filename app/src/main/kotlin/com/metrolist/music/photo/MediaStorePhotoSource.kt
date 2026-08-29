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
import android.provider.MediaStore
import androidx.core.content.ContextCompat
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

internal data class MediaStorePhotoPage(
    val photos: List<MediaStorePhoto>,
    val nextOffset: Int,
    val hasMore: Boolean,
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

    suspend fun loadPage(volume: MediaStoreVolume, offset: Int, limit: Int): MediaStorePhotoPage = withContext(Dispatchers.IO) {
        require(offset >= 0)
        require(limit > 0)
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(volume.name)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val signal = CancellationSignal()
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { signal.cancel() }
        try {
            val result = queryPage(collection, projection, offset, limit + 1, signal)
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

    private fun queryPage(
        collection: Uri,
        projection: Array<String>,
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
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC",
                    signal,
                ),
            )
            PageCursor(cursor, rowsToSkip = offset)
        }
    }

    private data class PageCursor(val cursor: Cursor, val rowsToSkip: Int)
}

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
