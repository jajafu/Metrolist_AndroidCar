/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:Suppress("LocalVariableName")

package com.metrolist.music.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    val requestedWidth = width ?: height!!
    val requestedHeight = height ?: width!!
    val isGoogleCdn = contains("googleusercontent.com") || contains("ggpht.com")

    if (isGoogleCdn) {
        val existingDimensions = Regex("=w(\\d+)-h(\\d+)").find(this)
        if (existingDimensions != null) {
            val originalWidth = existingDimensions.groupValues[1].toInt()
            val originalHeight = existingDimensions.groupValues[2].toInt()
            val resizedWidth = width ?: (requestedHeight * originalWidth / originalHeight)
            val resizedHeight = height ?: (requestedWidth * originalHeight / originalWidth)
            return replace(existingDimensions.value, "=w$resizedWidth-h$resizedHeight")
        }

        val baseUrl = split("=w", "=s", "=h", limit = 2)[0]
        return if (width != null && height != null) {
            "$baseUrl=w$requestedWidth-h$requestedHeight-p-l90-rj"
        } else {
            "$baseUrl=s${width ?: height}-p-l90-rj"
        }
    }

    if (contains("i.ytimg.com")) {
        return if (requestedWidth > 480) {
            replace("hqdefault.jpg", "maxresdefault.jpg")
                .replace("mqdefault.jpg", "maxresdefault.jpg")
                .replace("sddefault.jpg", "maxresdefault.jpg")
        } else if (requestedWidth > 320) {
            replace("mqdefault.jpg", "hqdefault.jpg")
        } else {
            this
        }
    }

    return this
}
