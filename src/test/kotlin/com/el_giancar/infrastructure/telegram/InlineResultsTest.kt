package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.URLInlineKeyboardButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InlineResultsTest {
    @Test
    fun `offers both games for an empty query`() {
        assertEquals(listOf("classic", "sheldon"), inlineResults("").map { it.id.toString() })
    }

    @Test
    fun `filters the result by game name`() {
        assertEquals(listOf("classic"), inlineResults("classic").map { it.id.toString() })
        assertEquals(listOf("sheldon"), inlineResults("lizard").map { it.id.toString() })
        assertEquals(listOf("sheldon"), inlineResults("SPOCK").map { it.id.toString() })
    }

    @Test
    fun `uses unique non-empty titles`() {
        val results = inlineResults("")

        assertEquals(results.size, results.map { it.title }.distinct().size)
        assertTrue(results.all { it.title.isNotBlank() })
        assertTrue(results.all { !it.description.isNullOrBlank() })
    }

    @Test
    fun `sends a message that links to the bot`() {
        for (result in inlineResults("")) {
            val content = assertIs<InputTextMessageContent>(result.inputMessageContent)
            assertTrue(content.text.contains(BOT_URL), content.text)
        }
    }

    @Test
    fun `attaches play and share buttons`() {
        for (result in inlineResults("")) {
            val buttons =
                result.replyMarkup
                    ?.keyboard
                    ?.single()
                    .orEmpty()

            assertTrue(
                buttons.any { it is URLInlineKeyboardButton && it.url == BOT_URL },
                "Missing Play button: $buttons",
            )
            assertTrue(
                buttons.any {
                    it is URLInlineKeyboardButton && it.url.startsWith("https://t.me/share/url?")
                },
                "Missing Share button: $buttons",
            )
        }
    }
}
