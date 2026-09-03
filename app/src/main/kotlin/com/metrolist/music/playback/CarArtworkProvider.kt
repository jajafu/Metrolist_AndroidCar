package com.metrolist.music.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/** Read-only artwork access is granted for each URI to the connected media browser. */
class CarArtworkProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "image/png"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val context = context ?: throw FileNotFoundException("Artwork provider unavailable")
        if (mode != "r" || uri.authority != "${context.packageName}.carArtwork") {
            throw FileNotFoundException("Invalid artwork request")
        }
        val source = uri.pathSegments.takeIf { it.size == 2 && it[0] == "image" }
            ?.get(1)?.let(Uri::parse)?.takeIf { isCarArtworkSource(it, context.packageName) }
            ?: throw FileNotFoundException("Invalid artwork source")
        if (!imagePermit.tryAcquire(10, TimeUnit.SECONDS)) {
            throw FileNotFoundException("Artwork provider busy")
        }
        try {
            val directory = File(context.cacheDir, "car-artwork").apply { mkdirs() }
            val key = MessageDigest.getInstance("SHA-256").digest(source.toString().toByteArray())
                .joinToString("") { "%02x".format(it) }
            val file = File(directory, "$key.png")
            if (!file.exists()) {
                val bitmap = runBlocking {
                    withTimeout(10_000) {
                        val result = context.imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(source)
                                .size(256, 256)
                                .scale(Scale.FIT)
                                .precision(Precision.EXACT)
                                .allowHardware(false)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .build()
                        ) as? SuccessResult ?: throw FileNotFoundException("Artwork unavailable")
                        result.image.toBitmap()
                    }
                }
                val temporary = File(directory, "$key.tmp")
                try {
                    temporary.outputStream().use { stream ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                    }
                    check(temporary.renameTo(file))
                } finally {
                    temporary.delete()
                }
            }
            file.setLastModified(System.currentTimeMillis())
            // The provider serializes decodes and keeps at most 96 small rendered covers on disk.
            directory.listFiles { candidate -> candidate.extension == "png" && candidate != file }
                ?.sortedByDescending(File::lastModified)?.drop(95)?.forEach(File::delete)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            throw FileNotFoundException("Unable to load artwork").apply { initCause(e) }
        } finally {
            imagePermit.release()
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()

    companion object {
        private val imagePermit = Semaphore(1, true)
    }
}

internal fun isCarArtworkSource(uri: Uri, packageName: String): Boolean =
    (uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) ||
        (uri.scheme == "content" && (
            (uri.authority == "media" && "albumart" in uri.pathSegments) ||
                uri.authority == "$packageName.FileProvider"
        ))

internal fun carArtworkUri(packageName: String, source: Uri?): Uri? {
    if (source == null || source.scheme == "android.resource") return source
    if (!isCarArtworkSource(source, packageName)) return null
    return Uri.Builder().scheme("content").authority("$packageName.carArtwork")
        .appendPath("image").appendPath(source.toString()).build()
}
