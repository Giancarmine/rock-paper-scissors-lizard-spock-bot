package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KeyboardsTest {
    @Test
    fun `share keyboard contains one URL button`() {
        val keyboard = shareKeyboard()
        val button = keyboard.keyboard.single().single()

        assertIs<URLInlineKeyboardButton>(button)
        assertTrue(button.url.startsWith("https://t.me/share/url?"), button.url)
    }

    @Test
    fun `share URL links to the bot start deep link`() {
        val url = shareUrl()

        assertTrue(
            url.contains("url=https%3A%2F%2Ft.me%2FRockPaperScissorsLizardSpockBot%3Fstart%3Dshare"),
            url,
        )
    }

    @Test
    fun `share URL encodes spaces as percent twenty`() {
        val url = shareUrl(url = "https://t.me/example", text = "a b")

        assertTrue(url.endsWith("text=a%20b"), url)
        assertFalse(url.contains("+"), url)
    }
}
