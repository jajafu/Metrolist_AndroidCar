/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime

class LikedSongsBackupManager(
    context: Context,
    private val database: MusicDatabase,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

    @OptIn(FlowPreview::class)
    suspend fun restoreAndObserve() {
        restoreBackup()

        database
            .likedSongsByNameAsc()
            .map { songs -> songs.map { song -> song.toBackupSong() } }
            .distinctUntilChanged()
            .debounce(BACKUP_DEBOUNCE_MS)
            .collect(::writeBackup)
    }

    private suspend fun restoreBackup() {
        val backup = readBackup() ?: return
        if (backup.version > BACKUP_VERSION) {
            Timber.w("Liked songs backup uses unsupported version ${backup.version}")
            return
        }

        var restoredCount = 0
        database.withTransaction {
            backup.songs.distinctBy(BackupSong::id).forEach { backupSong ->
                val likedDate = backupSong.likedDate.toLocalDateTimeOrNull() ?: LocalDateTime.now()
                val existing = songEntity(backupSong.id)
                if (existing == null) {
                    insert(backupSong.toMediaMetadata(likedDate))
                    restoredCount++
                } else if (!existing.liked || existing.likedDate == null || existing.inLibrary == null) {
                    update(
                        existing.copy(
                            liked = true,
                            likedDate = existing.likedDate ?: likedDate,
                            inLibrary = existing.inLibrary ?: likedDate,
                        ),
                    )
                    restoredCount++
                }
            }
        }

        Timber.i("Restored $restoredCount liked songs from $BACKUP_FILE_NAME")
    }

    private fun readBackup(): LikedSongsBackup? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return runCatching {
                val file = legacyBackupFile()
                if (file.isFile) json.decodeFromString<LikedSongsBackup>(file.readText()) else null
            }.onFailure { Timber.e(it, "Failed to read liked songs backup") }.getOrNull()
        }

        val backupUris = findBackupUris()
        backupUris.forEach { uri ->
            val backup =
                runCatching {
                    resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        json.decodeFromString<LikedSongsBackup>(reader.readText())
                    }
                }.onFailure { Timber.w(it, "Could not read liked songs backup at $uri") }
                    .getOrNull()
            if (backup != null) return backup
        }
        return null
    }

    private fun writeBackup(songs: List<BackupSong>) {
        val contents = json.encodeToString(LikedSongsBackup(songs = songs))
        val result =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeMediaStoreBackup(contents)
            } else {
                runCatching {
                    val file = legacyBackupFile()
                    file.parentFile?.mkdirs()
                    file.writeText(contents)
                }
            }

        result
            .onSuccess { Timber.d("Saved ${songs.size} liked songs to $BACKUP_FILE_NAME") }
            .onFailure { Timber.e(it, "Failed to save liked songs backup") }
    }

    private fun writeMediaStoreBackup(contents: String): Result<Unit> {
        findBackupUris().forEach { uri ->
            val result = writeToUri(uri, contents)
            if (result.isSuccess) {
                resolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
                    },
                    null,
                    null,
                )
                return result
            }
        }

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, BACKUP_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOAD_RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val uri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return Result.failure(IllegalStateException("Could not create liked songs backup"))

        val result = writeToUri(uri, contents)
        if (result.isSuccess) {
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
        } else {
            resolver.delete(uri, null, null)
        }
        return result
    }

    private fun writeToUri(
        uri: Uri,
        contents: String,
    ): Result<Unit> =
        runCatching {
            resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                writer.write(contents)
            } ?: error("Could not open liked songs backup for writing")
        }

    private fun findBackupUris(): List<Uri> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"

        return runCatching {
            resolver
                .query(
                    collection,
                    projection,
                    selection,
                    arrayOf(BACKUP_FILE_NAME, DOWNLOAD_RELATIVE_PATH),
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    buildList {
                        while (cursor.moveToNext()) {
                            add(ContentUris.withAppendedId(collection, cursor.getLong(idColumn)))
                        }
                    }
                }.orEmpty()
        }.onFailure { Timber.w(it, "Failed to locate liked songs backup") }.getOrDefault(emptyList())
    }

    @Suppress("DEPRECATION")
    private fun legacyBackupFile(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .resolve(BACKUP_FILE_NAME)

    private fun Song.toBackupSong() =
        BackupSong(
            id = song.id,
            title = song.title,
            artists =
                orderedArtists.map { artist ->
                    BackupArtist(id = artist.channelId ?: artist.id, name = artist.name)
                },
            duration = song.duration,
            thumbnailUrl = song.thumbnailUrl,
            albumId = song.albumId ?: album?.id,
            albumName = song.albumName ?: album?.title,
            explicit = song.explicit,
            likedDate = song.likedDate?.toString(),
            isVideo = song.isVideo,
        )

    private fun BackupSong.toMediaMetadata(likedDate: LocalDateTime) =
        MediaMetadata(
            id = id,
            title = title,
            artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            album =
                albumId?.let { id ->
                    MediaMetadata.Album(id = id, title = albumName.orEmpty())
                },
            musicVideoType = if (isVideo) "MUSIC_VIDEO_TYPE_OMV" else null,
            explicit = explicit,
            liked = true,
            likedDate = likedDate,
            inLibrary = likedDate,
        )

    private fun String?.toLocalDateTimeOrNull(): LocalDateTime? =
        this?.let { value -> runCatching { LocalDateTime.parse(value) }.getOrNull() }

    companion object {
        const val BACKUP_FILE_NAME = "Metrolist_liked_songs.json"
        private const val BACKUP_MIME_TYPE = "application/json"
        private const val BACKUP_VERSION = 1
        private const val BACKUP_DEBOUNCE_MS = 500L
        private val DOWNLOAD_RELATIVE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/"
    }
}

@Serializable
private data class LikedSongsBackup(
    val version: Int = 1,
    val songs: List<BackupSong>,
)

@Serializable
private data class BackupSong(
    val id: String,
    val title: String,
    val artists: List<BackupArtist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val explicit: Boolean = false,
    val likedDate: String? = null,
    val isVideo: Boolean = false,
)

@Serializable
private data class BackupArtist(
    val id: String? = null,
    val name: String,
)
