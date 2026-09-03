package com.metrolist.music.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.metrolist.music.db.entities.Song
import com.metrolist.music.utils.joinToArtistString

internal fun Song.toCarMediaItem(
    path: String,
    artistSeparator: String,
    isPlayable: Boolean = true,
    isBrowsable: Boolean = false,
): MediaItem {
    val artistText = orderedArtists.joinToArtistString(artistSeparator) { it.name }
    return MediaItem.Builder()
        .setMediaId("$path/$id")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setSubtitle(artistText)
                .setArtist(artistText)
                .setArtworkUri(song.thumbnailUrl?.toUri())
                .setIsPlayable(isPlayable)
                .setIsBrowsable(isBrowsable)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()
        ).build()
}
