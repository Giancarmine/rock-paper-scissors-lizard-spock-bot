package com.el_giancar.infrastructure.telegram

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotCommandsTest {
    @Test
    fun `registers every command once`() {
        val names = botCommands.map { it.command }

        assertEquals(names.distinct(), names)
    }

    @Test
    fun `registers every user-facing command in menu order`() {
        val expected =
            listOf(
                "start",
                "classic_game",
                "sheldon_game",
                "rules",
                "stats",
                "leaderboard",
                "chat_leaderboard",
                "help",
                "about",
            )

        assertEquals(expected, botCommands.map { it.command })
    }

    @Test
    fun `uses Telegram-compatible names and descriptions`() {
        for (command in botCommands) {
            assertTrue(command.command.matches(Regex("^[a-z0-9_]{1,32}$")), command.command)
            assertTrue(command.description.length in 1..256, command.description)
        }
    }
}
