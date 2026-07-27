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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime

internal fun isLikedSongsBackupFileName(displayName: String): Boolean =
    LIKED_SONGS_BACKUP_FILE_REGEX.matches(displayName)

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
            legacyBackupFiles().forEach { file ->
                val backup =
                    runCatching {
                        json.decodeFromString<LikedSongsBackup>(file.readText())
                    }.onFailure { Timber.w(it, "Could not read liked songs backup at ${file.path}") }
                        .getOrNull()
                if (backup != null) return backup
            }
            return null
        }

        findBackupEntries().forEach { entry ->
            val backup =
                runCatching {
                    resolver.openInputStream(entry.uri)?.bufferedReader()?.use { reader ->
                        json.decodeFromString<LikedSongsBackup>(reader.readText())
                    }
                }.onFailure { Timber.w(it, "Could not read liked songs backup at ${entry.uri}") }
                    .getOrNull()
            if (backup != null) return backup
        }
        return null
    }

    private suspend fun writeBackup(songs: List<BackupSong>) =
        backupWriteMutex.withLock {
            val contents = json.encodeToString(LikedSongsBackup(songs = songs))
            val result =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    writeMediaStoreBackup(contents)
                } else {
                    writeLegacyBackup(contents)
                }

            result
                .onSuccess { Timber.d("Saved ${songs.size} liked songs to $BACKUP_FILE_NAME") }
                .onFailure { Timber.e(it, "Failed to save liked songs backup") }
        }

    private fun writeMediaStoreBackup(contents: String): Result<Unit> =
        runCatching {
            val existingEntries = findBackupEntries()
            val currentEntries =
                existingEntries.sortedWith(
                    compareByDescending<BackupEntry> { entry ->
                        entry.displayName == BACKUP_FILE_NAME
                    }.thenByDescending(BackupEntry::dateModified),
                ).filter { entry ->
                    entry.displayName.startsWith(BACKUP_FILE_STEM)
                }

            writeFirstAvailable(currentEntries, contents)?.let { entry ->
                deleteDuplicateBackups(existingEntries, keepUri = entry.uri)
                return@runCatching
            }

            val createResult = createMediaStoreBackup(contents)
            createResult.getOrNull()?.let { uri ->
                deleteDuplicateBackups(findBackupEntries(), keepUri = uri)
                return@runCatching
            }

            val legacyEntries = existingEntries.filterNot { entry -> entry in currentEntries }
            writeFirstAvailable(legacyEntries, contents)?.let { entry ->
                deleteDuplicateBackups(existingEntries, keepUri = entry.uri)
                Timber.w(
                    createResult.exceptionOrNull(),
                    "Using legacy liked songs backup because the new backup file could not be created",
                )
                return@runCatching
            }

            throw createResult.exceptionOrNull()
                ?: IllegalStateException("Could not create or update liked songs backup")
        }

    private fun writeFirstAvailable(
        entries: List<BackupEntry>,
        contents: String,
    ): BackupEntry? {
        entries.forEach { entry ->
            val writeResult = runCatching { writeToUri(entry.uri, contents) }
            if (writeResult.isSuccess) {
                runCatching { updateMediaStoreEntry(entry.uri, isPending = false) }
                    .onFailure { error ->
                        Timber.w(error, "Could not update liked songs backup metadata at ${entry.uri}")
                    }
                return entry
            }
            Timber.w(writeResult.exceptionOrNull(), "Could not update liked songs backup at ${entry.uri}")
        }
        return null
    }

    private fun createMediaStoreBackup(contents: String): Result<Uri> =
        runCatching {
            val uri =
                resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILE_NAME)
                        put(MediaStore.MediaColumns.MIME_TYPE, BACKUP_MIME_TYPE)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOAD_RELATIVE_PATH)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    },
                ) ?: error("Could not create liked songs backup")

            try {
                writeToUri(uri, contents)
                updateMediaStoreEntry(uri, isPending = false)
                uri
            } catch (error: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                    .onFailure { cleanupError -> error.addSuppressed(cleanupError) }
                throw error
            }
        }

    private fun writeToUri(
        uri: Uri,
        contents: String,
    ) {
        resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(contents)
        } ?: error("Could not open liked songs backup for writing")
    }

    private fun updateMediaStoreEntry(
        uri: Uri,
        isPending: Boolean,
    ) {
        resolver.update(
            uri,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
                put(MediaStore.MediaColumns.IS_PENDING, if (isPending) 1 else 0)
            },
            null,
            null,
        )
    }

    private fun deleteDuplicateBackups(
        entries: List<BackupEntry>,
        keepUri: Uri,
    ) {
        entries
            .asSequence()
            .filterNot { entry -> entry.uri == keepUri }
            .forEach { entry ->
                runCatching { resolver.delete(entry.uri, null, null) }
                    .onSuccess { deleted ->
                        if (deleted > 0) {
                            Timber.i("Deleted duplicate liked songs backup ${entry.displayName}")
                        }
                    }.onFailure { error ->
                        Timber.w(error, "Could not delete duplicate liked songs backup ${entry.displayName}")
                    }
            }
    }

    private fun findBackupEntries(): List<BackupEntry> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND (" +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?)"

        return runCatching {
            resolver
                .query(
                    collection,
                    projection,
                    selection,
                    arrayOf(
                        DOWNLOAD_RELATIVE_PATH,
                        "$BACKUP_FILE_STEM%.json",
                        "$LEGACY_BACKUP_FILE_STEM%.json",
                    ),
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    buildList {
                        while (cursor.moveToNext()) {
                            val displayName = cursor.getString(nameColumn)
                            if (isLikedSongsBackupFileName(displayName)) {
                                add(
                                    BackupEntry(
                                        uri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)),
                                        displayName = displayName,
                                        dateModified = cursor.getLong(dateColumn),
                                    ),
                                )
                            }
                        }
                    }.sortedByDescending(BackupEntry::dateModified)
                }.orEmpty()
        }.onFailure { Timber.w(it, "Failed to locate liked songs backup") }.getOrDefault(emptyList())
    }

    private fun writeLegacyBackup(contents: String): Result<Unit> =
        runCatching {
            val backupFile = legacyDownloadsDirectory().resolve(BACKUP_FILE_NAME)
            backupFile.parentFile?.mkdirs()
            backupFile.writeText(contents)
            legacyBackupFiles()
                .filterNot { file -> file == backupFile }
                .forEach { file ->
                    runCatching {
                        if (!file.delete()) {
                            Timber.w("Could not delete duplicate liked songs backup ${file.path}")
                        }
                    }
                        .onFailure { error ->
                            Timber.w(error, "Could not delete duplicate liked songs backup ${file.path}")
                        }
                }
        }

    private fun legacyBackupFiles(): List<File> =
        legacyDownloadsDirectory()
            .listFiles { file -> file.isFile && isLikedSongsBackupFileName(file.name) }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()

    @Suppress("DEPRECATION")
    private fun legacyDownloadsDirectory(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

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
        const val BACKUP_FILE_NAME = "Metrolist_AndroidCar_liked_songs.json"
        private const val BACKUP_FILE_STEM = "Metrolist_AndroidCar_liked_songs"
        private const val LEGACY_BACKUP_FILE_STEM = "Metrolist_liked_songs"
        private const val BACKUP_MIME_TYPE = "application/json"
        private const val BACKUP_VERSION = 1
        private const val BACKUP_DEBOUNCE_MS = 500L
        private val DOWNLOAD_RELATIVE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/"
        private val backupWriteMutex = Mutex()
    }
}

private val LIKED_SONGS_BACKUP_FILE_REGEX =
    Regex(
        """^(?:Metrolist_AndroidCar_liked_songs|Metrolist_liked_songs)(?: \(\d+\))?\.json$""",
    )

private data class BackupEntry(
    val uri: Uri,
    val displayName: String,
    val dateModified: Long,
)

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
