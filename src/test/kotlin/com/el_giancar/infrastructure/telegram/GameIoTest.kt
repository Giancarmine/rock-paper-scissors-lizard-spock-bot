package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.ClassicMoveType
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameIoTest {
    @Test
    fun `only inline keyboards are attached after the reply keyboard is dismissed`() {
        assertNull(inlineKeyboardToAttach(null))
        assertNull(inlineKeyboardToAttach(commandsKeyboard()))
        assertNull(inlineKeyboardToAttach(movesKeyboard(ClassicMoveType.entries)))
        assertIs<InlineKeyboardMarkup>(inlineKeyboardToAttach(shareKeyboard()))
    }

    @Test
    fun `accepts a move from the player who started the game in the same chat`() {
        assertTrue(
            isMoveFromOriginatingPlayer(
                originChatId = -100L,
                originUserId = 42L,
                incomingChatId = -100L,
                incomingUserId = 42L,
            ),
        )
    }

    @Test
    fun `rejects a move from another member of the same group`() {
        assertFalse(
            isMoveFromOriginatingPlayer(
                originChatId = -100L,
                originUserId = 42L,
                incomingChatId = -100L,
                incomingUserId = 43L,
            ),
        )
    }

    @Test
    fun `rejects a move from the same player in another chat`() {
        assertFalse(
            isMoveFromOriginatingPlayer(
                originChatId = -100L,
                originUserId = 42L,
                incomingChatId = -200L,
                incomingUserId = 42L,
            ),
        )
    }

    @Test
    fun `rejects messages without an originating user`() {
        assertFalse(
            isMoveFromOriginatingPlayer(
                originChatId = -100L,
                originUserId = null,
                incomingChatId = -100L,
                incomingUserId = null,
            ),
        )
        assertFalse(
            isMoveFromOriginatingPlayer(
                originChatId = -100L,
                originUserId = null,
                incomingChatId = -100L,
                incomingUserId = 42L,
            ),
        )
    }
}
