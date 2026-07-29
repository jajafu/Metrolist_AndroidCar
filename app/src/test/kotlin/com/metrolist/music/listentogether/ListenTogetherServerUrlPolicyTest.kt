package com.metrolist.music.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenTogetherServerUrlPolicyTest {
    @Test
    fun `secure remote websocket is allowed`() {
        assertTrue(ListenTogetherServerUrlPolicy.isAllowed("wss://example.com/ws"))
    }

    @Test
    fun `cleartext websocket is limited to local network hosts`() {
        assertTrue(ListenTogetherServerUrlPolicy.isAllowed("ws://192.168.1.20:8080/ws"))
        assertTrue(ListenTogetherServerUrlPolicy.isAllowed("ws://10.0.0.4/ws"))
        assertTrue(ListenTogetherServerUrlPolicy.isAllowed("ws://server.local:8080/ws"))
        assertTrue(ListenTogetherServerUrlPolicy.isAllowed("ws://[fd12:3456::1]:8080/ws"))

        assertFalse(ListenTogetherServerUrlPolicy.isAllowed("ws://example.com/ws"))
        assertFalse(ListenTogetherServerUrlPolicy.isAllowed("ws://8.8.8.8/ws"))
        assertFalse(ListenTogetherServerUrlPolicy.isAllowed("ws://unqualified-host/ws"))
    }

    @Test
    fun `non websocket and credentialed URLs are rejected`() {
        assertFalse(ListenTogetherServerUrlPolicy.isAllowed("https://example.com/ws"))
        assertFalse(ListenTogetherServerUrlPolicy.isAllowed("wss://user:password@example.com/ws"))
        assertFalse(ListenTogetherServerUrlPolicy.isAllowed("not a URL"))
    }

    @Test
    fun `invalid configured URL falls back to secure default`() {
        val defaultUrl = "wss://default.example/ws"

        assertEquals(
            defaultUrl,
            ListenTogetherServerUrlPolicy.normalizeOrDefault("ws://example.com/ws", defaultUrl),
        )
    }
}
