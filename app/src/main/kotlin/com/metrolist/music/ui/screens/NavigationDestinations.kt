/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import kotlinx.serialization.Serializable

@Serializable
data class ArtistItemsDestination(
    val artistId: String,
    val browseId: String,
    val params: String? = null,
)

@Serializable
data class YouTubeBrowseDestination(
    val browseId: String,
    val params: String? = null,
)
