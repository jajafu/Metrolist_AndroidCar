package com.metrolist.music.playback

import androidx.media3.common.MediaItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.SearchSummary
import com.metrolist.music.models.MediaMetadata

internal fun rankVoiceSearchSongs(
    summaries: List<SearchSummary>,
    topResultSectionTitle: String,
): List<SongItem> =
    summaries
        .asSequence()
        .filterNot { it.title == topResultSectionTitle }
        .flatMap { it.items.asSequence() }
        .filterIsInstance<SongItem>()
        .filterNot(SongItem::isEpisode)
        .distinctBy(SongItem::id)
        .toList()

internal fun orderSearchMetadata(
    online: List<MediaMetadata>,
    local: List<MediaMetadata>,
    prioritizeOnline: Boolean,
): List<MediaMetadata> =
    (if (prioritizeOnline) online + local else local + online)
        .distinctBy(MediaMetadata::id)

internal data class VoiceRadioMediaPlan(
    val items: List<MediaItem>,
    val startIndex: Int,
)

internal fun buildVoiceRadioMediaPlan(
    seed: MediaItem,
    radioItems: List<MediaItem>,
): VoiceRadioMediaPlan =
    VoiceRadioMediaPlan(
        items = listOf(seed) + radioItems.filterNot { it.mediaId == seed.mediaId },
        startIndex = 0,
    )
