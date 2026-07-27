package com.metrolist.music.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LikedSongsBackupManagerTest {
    @Test
    fun `recognizes current backup names and MediaStore suffixes`() {
        assertTrue(isLikedSongsBackupFileName("Metrolist_AndroidCar_liked_songs.json"))
        assertTrue(isLikedSongsBackupFileName("Metrolist_AndroidCar_liked_songs (1).json"))
        assertTrue(isLikedSongsBackupFileName("Metrolist_AndroidCar_liked_songs (37).json"))
    }

    @Test
    fun `recognizes legacy backup names for migration`() {
        assertTrue(isLikedSongsBackupFileName("Metrolist_liked_songs.json"))
        assertTrue(isLikedSongsBackupFileName("Metrolist_liked_songs (12).json"))
    }

    @Test
    fun `rejects unrelated or malformed download files`() {
        assertFalse(isLikedSongsBackupFileName("Metrolist_AndroidCar_liked_songs.json.bak"))
        assertFalse(isLikedSongsBackupFileName("Metrolist_AndroidCar_liked_songs (copy).json"))
        assertFalse(isLikedSongsBackupFileName("Metrolist_liked_songs_1.json"))
        assertFalse(isLikedSongsBackupFileName("another_liked_songs.json"))
    }
}
