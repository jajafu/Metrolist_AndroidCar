package com.metrolist.music.viewmodels

import java.util.Locale

internal fun shouldHideHomeSection(
    title: String,
    browseId: String?,
): Boolean {
    if (browseId == "FEmusic_history") return true

    val normalizedTitle = title.trim().lowercase(Locale.ROOT)
    if (normalizedTitle in listenAgainTitles) return true

    val isEnglishCoversAndRemixes =
        "cover" in normalizedTitle && "remix" in normalizedTitle
    val isChineseCoversAndRemixes =
        "翻唱" in normalizedTitle &&
            ("重混" in normalizedTitle || "混音" in normalizedTitle)

    return isEnglishCoversAndRemixes || isChineseCoversAndRemixes
}

private val listenAgainTitles =
    setOf(
        "listen again",
        "再聽一次",
        "再听一次",
    )
