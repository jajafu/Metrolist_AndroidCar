package com.metrolist.music.photo

import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class DirectUsbRoot(val name: String, val path: String)

internal data class DirectUsbDirectory(val name: String, val path: String)

internal data class DirectUsbListing(
    val directory: DirectUsbDirectory,
    val directories: List<DirectUsbDirectory>,
    val photos: List<MediaStorePhoto>,
)

internal class DirectUsbPhotoSource(rootPath: String) {
    private val root = File(rootPath).canonicalFile

    suspend fun loadDirectory(path: String): DirectUsbListing = withContext(Dispatchers.IO) {
        val directory = checkedDirectory(path)
        val entries = directory.listFiles() ?: throw IOException("USB directory cannot be read")
        val folders = ArrayList<DirectUsbDirectory>()
        val photos = ArrayList<MediaStorePhoto>()
        entries.forEach { entry ->
            currentCoroutineContext().ensureActive()
            val checked = checkedChild(entry) ?: return@forEach
            when {
                checked.isDirectory && checked.canRead() -> folders += DirectUsbDirectory(checked.name, checked.absolutePath)
                checked.isFile && checked.canRead() && isSupportedDirectImage(checked) -> photos += MediaStorePhoto(
                    uri = Uri.fromFile(checked).toString(),
                    name = checked.name,
                )
            }
        }
        val comparator = compareBy<String> { it.lowercase(Locale.ROOT) }
        DirectUsbListing(
            directory = DirectUsbDirectory(directory.name, directory.absolutePath),
            directories = folders.sortedWith(compareBy(comparator) { it.name }),
            photos = photos.sortedWith(compareBy(comparator) { it.name }),
        )
    }

    suspend fun loadFolderPhotos(path: String, progress: (Int) -> Unit): List<MediaStorePhoto> = withContext(Dispatchers.IO) {
        val directories = ArrayDeque<File>().apply { add(checkedDirectory(path)) }
        val visited = hashSetOf<String>()
        val photos = linkedMapOf<String, MediaStorePhoto>()
        while (directories.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val directory = directories.removeFirst()
            if (!visited.add(directory.absolutePath)) continue
            directory.listFiles()?.forEach { entry ->
                currentCoroutineContext().ensureActive()
                val checked = checkedChild(entry) ?: return@forEach
                when {
                    checked.isDirectory && checked.canRead() -> directories.addLast(checked)
                    checked.isFile && checked.canRead() && isSupportedDirectImage(checked) -> {
                        val uri = Uri.fromFile(checked).toString()
                        photos.putIfAbsent(uri, MediaStorePhoto(uri, checked.name))
                    }
                }
            }
            progress(photos.size)
        }
        photos.values.toList()
    }

    fun parentOf(path: String): String? {
        val directory = checkedDirectory(path)
        return directory.parentFile?.takeIf { isWithinRoot(it) }?.absolutePath
    }

    private fun checkedDirectory(path: String): File {
        val directory = File(path).canonicalFile
        if (!isWithinRoot(directory) || !directory.isDirectory || !directory.canRead()) {
            throw IOException("USB directory is outside the mounted storage root")
        }
        return directory
    }

    private fun checkedChild(file: File): File? = runCatching { file.canonicalFile }.getOrNull()
        ?.takeIf(::isWithinRoot)

    private fun isWithinRoot(file: File): Boolean {
        val checked = runCatching { file.canonicalFile }.getOrNull() ?: return false
        return checked == root || checked.path.startsWith(root.path + File.separator)
    }
}

internal fun isSupportedDirectImage(file: File): Boolean = file.extension.lowercase(Locale.ROOT) in DirectImageExtensions

private val DirectImageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")
