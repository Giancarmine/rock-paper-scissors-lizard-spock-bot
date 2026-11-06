package com.el_giancar.infrastructure.telegram

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdleTextTest {
    @Test
    fun `hints on plain text in an idle private chat`() {
        assertTrue(shouldHintOnIdleText("hello", chatIsBusy = false, chatIsPrivate = true))
        assertTrue(shouldHintOnIdleText("  Rock  ", chatIsBusy = false, chatIsPrivate = true))
    }

    @Test
    fun `does not hint for commands`() {
        assertFalse(shouldHintOnIdleText("/start", chatIsBusy = false, chatIsPrivate = true))
        assertFalse(shouldHintOnIdleText("  /classic_game", chatIsBusy = false, chatIsPrivate = true))
    }

    @Test
    fun `does not hint for blank text`() {
        assertFalse(shouldHintOnIdleText("", chatIsBusy = false, chatIsPrivate = true))
        assertFalse(shouldHintOnIdleText("   ", chatIsBusy = false, chatIsPrivate = true))
    }

    @Test
    fun `does not hint while a game holds the chat`() {
        assertFalse(shouldHintOnIdleText("Rock", chatIsBusy = true, chatIsPrivate = true))
    }

    @Test
    fun `does not hint in a group chat`() {
        assertFalse(shouldHintOnIdleText("hello", chatIsBusy = false, chatIsPrivate = false))
    }
}
