/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import com.metrolist.music.db.entities.Song

internal fun resolveCurrentHomeSong(
    updatedSong: Song?,
    originalSong: Song,
): Song = updatedSong ?: originalSong
