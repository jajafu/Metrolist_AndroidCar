package com.metrolist.music.playback

import androidx.media3.common.MediaItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.SearchSummary
import com.metrolist.music.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSearchPolicyTest {
    @Test
    fun `summary ranking is deterministic and skips non-song top result`() {
        val summaries =
            listOf(
                SearchSummary(
                    title = "Top result",
                    items =
                        listOf(
                            song("video-top"),
                            ArtistItem(
                                id = "artist",
                                title = "Artist",
                                thumbnail = null,
                                shuffleEndpoint = null,
                                radioEndpoint = null,
                            ),
                        ),
                ),
                SearchSummary(
                    title = "Songs",
                    items = listOf(song("first"), song("second"), song("first")),
                ),
            )

        val firstRun = rankVoiceSearchSongs(summaries, "Top result").map(SongItem::id)
        val secondRun = rankVoiceSearchSongs(summaries, "Top result").map(SongItem::id)

        assertEquals(listOf("first", "second"), firstRun)
        assertEquals(firstRun, secondRun)
    }

    @Test
    fun `cold database cannot drop direct online result`() {
        val online = listOf(metadata("online-top"))

        val ordered =
            orderSearchMetadata(
                online = online,
                local = emptyList(),
                prioritizeOnline = true,
            )

        assertEquals(listOf("online-top"), ordered.map(MediaMetadata::id))
    }

    @Test
    fun `voice prioritizes online relevance while taps preserve local order`() {
        val online = listOf(metadata("online"), metadata("duplicate"))
        val local = listOf(metadata("local"), metadata("duplicate"))

        assertEquals(
            listOf("online", "duplicate", "local"),
            orderSearchMetadata(online, local, prioritizeOnline = true).map(MediaMetadata::id),
        )
        assertEquals(
            listOf("local", "duplicate", "online"),
            orderSearchMetadata(online, local, prioritizeOnline = false).map(MediaMetadata::id),
        )
    }

    @Test
    fun `empty search results remain empty`() {
        assertTrue(
            orderSearchMetadata(
                online = emptyList(),
                local = emptyList(),
                prioritizeOnline = true,
            ).isEmpty(),
        )
    }

    @Test
    fun `radio plan starts with selected result and keeps continuation items`() {
        val seed = mediaItem("seed")
        val plan =
            buildVoiceRadioMediaPlan(
                seed = seed,
                radioItems = listOf(mediaItem("related-1"), seed, mediaItem("related-2")),
            )

        assertEquals(0, plan.startIndex)
        assertEquals(
            listOf("seed", "related-1", "related-2"),
            plan.items.map(MediaItem::mediaId),
        )
    }

    private fun song(id: String) =
        SongItem(
            id = id,
            title = id,
            artists = listOf(Artist(name = "artist", id = "artist")),
            thumbnail = "https://example.com/$id.jpg",
        )

    private fun metadata(id: String) =
        MediaMetadata(
            id = id,
            title = id,
            artists = listOf(MediaMetadata.Artist(id = "artist", name = "artist")),
            duration = 180,
            thumbnailUrl = null,
        )

    private fun mediaItem(id: String) = MediaItem.Builder().setMediaId(id).build()
}
