package com.metrolist.music.photo

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class FrameManifest(
    val schemaVersion: Int = 1,
    val photos: List<FramePhoto> = emptyList(),
    val scannedFolders: Set<String> = emptySet(),
)

internal data class ManifestRead(val manifest: FrameManifest, val damaged: Boolean = false)

internal class PhotoFrameManifest(
    private val file: File,
    private val replace: (File, File) -> Unit = { temporary, destination ->
        Files.move(
            temporary.toPath(),
            destination.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    },
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun read(): ManifestRead {
        if (!file.exists()) return ManifestRead(FrameManifest())
        return try {
            val manifest = json.decodeFromString<FrameManifest>(file.readText())
            if (manifest.schemaVersion != 1 || manifest.photos.any { it.uri.isBlank() || it.sourceUri.isBlank() }) {
                ManifestRead(FrameManifest(), damaged = true)
            } else {
                ManifestRead(manifest.copy(photos = manifest.photos.distinctBy { it.sourceUri to it.uri }))
            }
        } catch (_: SerializationException) {
            ManifestRead(FrameManifest(), damaged = true)
        } catch (_: IllegalArgumentException) {
            ManifestRead(FrameManifest(), damaged = true)
        }
    }

    fun write(manifest: FrameManifest) {
        val directory = requireNotNull(file.parentFile)
        if (!directory.isDirectory && !directory.mkdirs()) throw java.io.IOException("Cannot create photo index directory")
        val temporary = File.createTempFile(file.name, ".tmp", directory)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            replace(temporary, file)
        } finally {
            temporary.delete()
        }
    }
}
