package com.metrolist.music.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class BackupRestoreFilesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test(expected = IOException::class)
    fun `null backup output stream fails`() {
        val source = temporaryFolder.newFile("settings.preferences_pb").apply { writeText("settings") }

        writeBackupArchive(
            destination = null,
            entries = listOf(BackupArchiveEntry("settings.preferences_pb", source)),
        )
    }

    @Test(expected = IOException::class)
    fun `backup write failure is propagated`() {
        val source = temporaryFolder.newFile("database.db").apply { writeText("database") }
        val failingOutput =
            object : OutputStream() {
                override fun write(value: Int) {
                    throw IOException("simulated write failure")
                }
            }

        writeBackupArchive(
            destination = failingOutput,
            entries = listOf(BackupArchiveEntry("database.db", source)),
        )
    }

    @Test(expected = IOException::class)
    fun `null restore input stream fails`() {
        extractBackupArchive(
            source = null,
            destinations = mapOf("database.db" to temporaryFolder.newFile("restored.db")),
        )
    }

    @Test
    fun `staged files replace all live files`() {
        val directory = temporaryFolder.newFolder("success")
        val database = File(directory, "song.db").apply { writeText("old-db") }
        val settings = File(directory, "settings.preferences_pb").apply { writeText("old-settings") }
        val stagedDatabase = File(directory, "song.db.restore_staged").apply { writeText("new-db") }
        val stagedSettings = File(directory, "settings.restore_staged.preferences_pb").apply { writeText("new-settings") }

        promoteStagedFiles(
            listOf(
                StagedFileReplacement(stagedDatabase, database),
                StagedFileReplacement(stagedSettings, settings),
            ),
        )

        assertEquals("new-db", database.readText())
        assertEquals("new-settings", settings.readText())
        assertFalse(File("${database.absolutePath}.restore_backup").exists())
        assertFalse(File("${settings.absolutePath}.restore_backup").exists())
    }

    @Test
    fun `failed second promotion rolls back every live file`() {
        val directory = temporaryFolder.newFolder("rollback")
        val database = File(directory, "song.db").apply { writeText("old-db") }
        val settings = File(directory, "settings.preferences_pb").apply { writeText("old-settings") }
        val stagedDatabase = File(directory, "song.db.restore_staged").apply { writeText("new-db") }
        val stagedSettings = File(directory, "settings.restore_staged.preferences_pb").apply { writeText("new-settings") }

        val result =
            runCatching {
                promoteStagedFiles(
                    replacements =
                        listOf(
                            StagedFileReplacement(stagedDatabase, database),
                            StagedFileReplacement(stagedSettings, settings),
                        ),
                    moveFile = { source, target ->
                        if (source == stagedSettings && target == settings) {
                            throw IOException("simulated promotion failure")
                        }
                        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    },
                )
            }

        assertTrue(result.isFailure)
        assertEquals("old-db", database.readText())
        assertEquals("old-settings", settings.readText())
    }
}
