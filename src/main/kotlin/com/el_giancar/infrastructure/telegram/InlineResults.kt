package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId

/**
 * Builds the inline results for `@RockPaperScissorsLizardSpockBot <query>`.
 *
 * The results send a short message with a Play button and a Share button, so a user
 * can drop the bot into any chat without leaving the conversation. The query only
 * picks a variant; it never changes the text or the links.
 */
internal fun inlineResults(query: String): List<InlineQueryResultArticle> {
    val normalized = query.trim().lowercase()
    return when {
        normalized.contains("classic") -> listOf(classicInlineResult())
        normalized.contains("sheldon") ||
            normalized.contains("lizard") ||
            normalized.contains("spock") -> listOf(sheldonInlineResult())
        else -> listOf(classicInlineResult(), sheldonInlineResult())
    }
}

private fun classicInlineResult(): InlineQueryResultArticle =
    InlineQueryResultArticle(
        id = InlineQueryId("classic"),
        title = "Play Rock–Paper–Scissors",
        inputMessageContent =
            InputTextMessageContent(
                "🎮 Let's play Rock–Paper–Scissors! Start a round with me: $BOT_URL",
            ),
        replyMarkup = inlinePlayKeyboard(),
        description = "Send a message that opens the bot",
    )

private fun sheldonInlineResult(): InlineQueryResultArticle =
    InlineQueryResultArticle(
        id = InlineQueryId("sheldon"),
        title = "Play Rock–Paper–Scissors–Lizard–Spock",
        inputMessageContent =
            InputTextMessageContent(
                "🎮 Let's play Rock–Paper–Scissors–Lizard–Spock! Start a round with me: $BOT_URL",
            ),
        replyMarkup = inlinePlayKeyboard(),
        description = "Send a message that opens the bot",
    )
