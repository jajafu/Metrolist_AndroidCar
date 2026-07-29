/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.listentogether

import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

internal object ListenTogetherServerUrlPolicy {
    fun isAllowed(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.removeSurrounding("[", "]") ?: return false
        if (uri.userInfo != null || host.isBlank()) return false

        return when (scheme) {
            "wss" -> true
            "ws" -> isLocalNetworkHost(host)
            else -> false
        }
    }

    fun normalizeOrDefault(
        url: String,
        defaultUrl: String,
    ): String {
        val trimmed = url.trim()
        return trimmed.takeIf(::isAllowed) ?: defaultUrl
    }

    private fun isLocalNetworkHost(host: String): Boolean {
        val normalizedHost = host.lowercase().trimEnd('.')
        if (normalizedHost == "localhost" ||
            normalizedHost.endsWith(".localhost") ||
            normalizedHost.endsWith(".local") ||
            normalizedHost.endsWith(".lan") ||
            normalizedHost.endsWith(".home.arpa")
        ) {
            return true
        }

        parseIpv4(normalizedHost)?.let { octets ->
            return octets[0] == 10 ||
                octets[0] == 127 ||
                (octets[0] == 169 && octets[1] == 254) ||
                (octets[0] == 172 && octets[1] in 16..31) ||
                (octets[0] == 192 && octets[1] == 168)
        }

        if (':' !in normalizedHost) return false
        val address =
            runCatching { InetAddress.getByName(normalizedHost) }.getOrNull() as? Inet6Address
                ?: return false
        val firstByte = address.address.first().toInt() and 0xff
        return address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            firstByte and 0xfe == 0xfc
    }

    private fun parseIpv4(host: String): IntArray? {
        val parts = host.split('.')
        if (parts.size != 4) return null
        return parts
            .map { part ->
                if (part.isEmpty() || part.length > 3 || part.any { !it.isDigit() }) return null
                part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
            }.toIntArray()
    }
}
