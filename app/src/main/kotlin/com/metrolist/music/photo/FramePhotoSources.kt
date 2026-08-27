package com.metrolist.music.photo

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine

internal data class FrameDocument(val uri: String, val name: String, val mimeType: String) {
    val isDirectory: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    val isImage: Boolean get() = mimeType.startsWith("image/", ignoreCase = true)
}

internal class InvalidFrameImageException : IOException()

internal interface FrameDocumentAccess {
    fun hasPersistedRead(uri: Uri): Boolean
    fun persistRead(uri: Uri): Boolean
    suspend fun picked(uri: Uri): FrameDocument
    suspend fun folder(uri: Uri): FrameDocument
    suspend fun children(treeUri: Uri, directoryUri: String): List<FrameDocument>
}

internal class AndroidFrameDocumentAccess(private val resolver: ContentResolver) : FrameDocumentAccess {
    override fun hasPersistedRead(uri: Uri): Boolean =
        resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

    override fun persistRead(uri: Uri): Boolean = try {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    override suspend fun picked(uri: Uri): FrameDocument {
        requireContentUri(uri)
        val type = resolver.getType(uri) ?: throw InvalidFrameImageException()
        if (!type.startsWith("image/", ignoreCase = true)) throw InvalidFrameImageException()
        return queryCancellable { signal ->
            resolver.openAssetFileDescriptor(uri, "r", signal)?.use { } ?: throw FileNotFoundException()
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null, signal)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            FrameDocument(uri.toString(), name ?: uri.lastPathSegment.orEmpty(), type)
        }
    }

    override suspend fun folder(uri: Uri): FrameDocument {
        requireContentUri(uri)
        if (!DocumentsContract.isTreeUri(uri)) throw IllegalArgumentException("Not a tree URI")
        val document = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        return queryCancellable { signal ->
            resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null, signal)?.use { cursor ->
                if (!cursor.moveToFirst()) throw FileNotFoundException()
                FrameDocument(document.toString(), cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty())
                    .also { if (!it.isDirectory) throw IOException("Selected document is not a folder") }
            } ?: throw FileNotFoundException()
        }
    }

    override suspend fun children(treeUri: Uri, directoryUri: String): List<FrameDocument> {
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getDocumentId(directoryUri.toUri()),
        )
        return queryCancellable { signal ->
            resolver.query(
                childUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null,
                null,
                null,
                signal,
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        signal.throwIfCanceled()
                        val documentId = cursor.getString(0) ?: continue
                        add(
                            FrameDocument(
                                DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString(),
                                cursor.getString(1).orEmpty(),
                                cursor.getString(2).orEmpty(),
                            ),
                        )
                    }
                }
            } ?: throw FileNotFoundException()
        }
    }

    private fun requireContentUri(uri: Uri) {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Photo selection must use a content URI" }
    }

    private suspend fun <T> queryCancellable(block: (CancellationSignal) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                val result = block(signal)
                if (continuation.isActive) continuation.resume(result)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
}

internal suspend fun scanFrameFolder(
    sourceUri: String,
    rootDocumentUri: String,
    children: suspend (String) -> List<FrameDocument>,
    progress: (Int) -> Unit,
): List<FramePhoto> {
    val directories = ArrayDeque<String>().apply { add(rootDocumentUri) }
    val visited = hashSetOf<String>()
    val photos = linkedMapOf<String, FramePhoto>()
    while (directories.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        val directory = directories.removeFirst()
        if (!visited.add(directory)) continue
        for (document in children(directory)) {
            currentCoroutineContext().ensureActive()
            when {
                document.isDirectory -> directories.addLast(document.uri)
                document.isImage -> photos.putIfAbsent(document.uri, FramePhoto(document.uri, sourceUri))
            }
            if (photos.isNotEmpty() && photos.size % 100 == 0) progress(photos.size)
        }
        progress(photos.size)
    }
    return photos.values.toList()
}
