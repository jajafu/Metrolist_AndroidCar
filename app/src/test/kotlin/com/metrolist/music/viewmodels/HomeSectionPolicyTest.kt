package com.metrolist.music.viewmodels

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionPolicyTest {
    @Test
    fun hidesHistorySectionByStableBrowseId() {
        assertTrue(
            shouldHideHomeSection(
                title = "Recently played",
                browseId = "FEmusic_history",
            ),
        )
    }

    @Test
    fun hidesListenAgainTitles() {
        assertTrue(shouldHideHomeSection("Listen again", browseId = null))
        assertTrue(shouldHideHomeSection("再聽一次", browseId = null))
        assertTrue(shouldHideHomeSection("再听一次", browseId = null))
    }

    @Test
    fun hidesCoversAndRemixesTitles() {
        assertTrue(shouldHideHomeSection("Covers & remixes", browseId = null))
        assertTrue(shouldHideHomeSection("翻唱與重混", browseId = null))
        assertTrue(shouldHideHomeSection("翻唱和混音", browseId = null))
    }

    @Test
    fun keepsOtherRecommendationSections() {
        assertFalse(shouldHideHomeSection("New releases", browseId = null))
        assertFalse(shouldHideHomeSection("熱門歌曲", browseId = null))
    }
}
