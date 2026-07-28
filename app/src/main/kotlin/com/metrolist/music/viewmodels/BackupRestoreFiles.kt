/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class BackupArchiveEntry(
    val name: String,
    val source: File,
)

internal data class StagedFileReplacement(
    val staged: File,
    val target: File,
)

internal fun writeBackupArchive(
    destination: OutputStream?,
    entries: List<BackupArchiveEntry>,
) {
    val output = destination ?: throw IOException("Could not open backup destination")
    require(entries.isNotEmpty()) { "Backup has no entries" }

    output.buffered().use { buffered ->
        ZipOutputStream(buffered).use { zip ->
            entries.forEach { entry ->
                require(entry.source.isFile && entry.source.length() > 0L) {
                    "Backup source is missing or empty: ${entry.source}"
                }
                zip.putNextEntry(ZipEntry(entry.name))
                entry.source.inputStream().buffered().use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }
    }
}

internal fun extractBackupArchive(
    source: InputStream?,
    destinations: Map<String, File>,
): Set<String> {
    val input = source ?: throw IOException("Could not open backup source")
    val extractedEntries = mutableSetOf<String>()

    input.buffered().use { buffered ->
        ZipInputStream(buffered).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val destination = destinations[entry.name]
                if (destination != null) {
                    require(extractedEntries.add(entry.name)) {
                        "Backup contains duplicate entry: ${entry.name}"
                    }
                    destination.parentFile?.mkdirs()
                    destination.outputStream().buffered().use { output ->
                        zip.copyTo(output)
                    }
                    require(destination.length() > 0L) {
                        "Backup entry is empty: ${entry.name}"
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    require(extractedEntries.isNotEmpty()) { "Backup contains no supported entries" }
    return extractedEntries
}

internal fun copyFileVerified(
    source: File,
    target: File,
) {
    require(source.isFile && source.length() > 0L) {
        "Restore source is missing or empty: $source"
    }
    target.parentFile?.mkdirs()
    source.inputStream().buffered().use { input ->
        target.outputStream().buffered().use { output ->
            input.copyTo(output)
        }
    }
    require(target.isFile && target.length() == source.length()) {
        "Restore staging copy is incomplete: $target"
    }
}

internal fun promoteStagedFiles(
    replacements: List<StagedFileReplacement>,
    moveFile: (File, File) -> Unit = ::moveFileReplacing,
) {
    require(replacements.isNotEmpty()) { "Restore has no staged files" }
    require(replacements.map { it.target.absolutePath }.distinct().size == replacements.size) {
        "Restore contains duplicate targets"
    }
    replacements.forEach { replacement ->
        require(replacement.staged.isFile && replacement.staged.length() > 0L) {
            "Restore staging file is missing or empty: ${replacement.staged}"
        }
        require(replacement.staged.absolutePath != replacement.target.absolutePath) {
            "Restore staging file cannot be the live target"
        }
    }

    data class AppliedReplacement(
        val replacement: StagedFileReplacement,
        val backup: File,
        val hadOriginal: Boolean,
    )

    val applied = mutableListOf<AppliedReplacement>()
    try {
        replacements.forEach { replacement ->
            replacement.target.parentFile?.mkdirs()
            val backup = File("${replacement.target.absolutePath}.restore_backup")
            recoverOrDiscardStaleBackup(replacement.target, backup, moveFile)

            val hadOriginal = replacement.target.exists()
            if (hadOriginal) {
                moveFile(replacement.target, backup)
            }
            applied += AppliedReplacement(replacement, backup, hadOriginal)
            moveFile(replacement.staged, replacement.target)
        }
    } catch (error: Throwable) {
        applied.asReversed().forEach { appliedReplacement ->
            val target = appliedReplacement.replacement.target
            runCatching {
                if (target.exists() && !target.delete()) {
                    throw IOException("Could not remove failed restore target: $target")
                }
                if (appliedReplacement.hadOriginal && appliedReplacement.backup.exists()) {
                    restoreBackupFile(appliedReplacement.backup, target, moveFile)
                }
            }.onFailure(error::addSuppressed)
        }
        throw error
    }

    applied.forEach { appliedReplacement ->
        if (appliedReplacement.backup.exists() && !appliedReplacement.backup.delete()) {
            appliedReplacement.backup.deleteOnExit()
        }
    }
}

private fun recoverOrDiscardStaleBackup(
    target: File,
    backup: File,
    moveFile: (File, File) -> Unit,
) {
    if (!backup.exists()) return
    if (target.exists()) {
        if (!backup.delete()) {
            throw IOException("Could not remove stale restore backup: $backup")
        }
    } else {
        restoreBackupFile(backup, target, moveFile)
    }
}

private fun restoreBackupFile(
    backup: File,
    target: File,
    moveFile: (File, File) -> Unit,
) {
    runCatching {
        moveFile(backup, target)
    }.recoverCatching {
        copyFileVerified(backup, target)
        if (!backup.delete()) {
            backup.deleteOnExit()
        }
    }.getOrThrow()
}

private fun moveFileReplacing(
    source: File,
    target: File,
) {
    try {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }
}
