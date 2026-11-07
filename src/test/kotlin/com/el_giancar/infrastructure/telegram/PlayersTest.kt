package com.el_giancar.infrastructure.telegram

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayersTest {
    @Test
    fun `prefers the full Telegram name`() {
        assertEquals("Alice Cooper", playerDisplayName("Alice", "Cooper", "alice"))
    }

    @Test
    fun `falls back to the username without the at sign`() {
        assertEquals("alice", playerDisplayName(null, null, "@alice"))
    }

    @Test
    fun `uses a generic label when the user has no name or username`() {
        assertEquals("Player", playerDisplayName(null, null, null))
        assertEquals("Player", playerDisplayName(" ", "", " "))
    }

    @Test
    fun `truncates names to the database column length`() {
        assertEquals(255, playerDisplayName("x".repeat(300), null, null).length)
    }
}
