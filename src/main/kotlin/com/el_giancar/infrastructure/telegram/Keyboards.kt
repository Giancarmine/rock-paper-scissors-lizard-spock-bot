package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.Move
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.urlInlineButton
import dev.inmo.tgbotapi.utils.row
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun commandsKeyboard(): ReplyKeyboardMarkup =
    replyKeyboard(
        resizeKeyboard = true,
        oneTimeKeyboard = true,
    ) {
        row {
            simpleButton("/classic_game")
            simpleButton("/sheldon_game")
            simpleButton("/rules")
        }
        row {
            simpleButton("/stats")
            simpleButton("/leaderboard")
            simpleButton("/chat_leaderboard")
        }
        row {
            simpleButton("/help")
            simpleButton("/commands")
            simpleButton("/about")
        }
    }

internal fun <M : Move> movesKeyboard(moves: List<M>): ReplyKeyboardMarkup =
    replyKeyboard(
        resizeKeyboard = true,
        oneTimeKeyboard = true,
    ) {
        moves.forEach { move ->
            row {
                simpleButton(move.displayName)
            }
        }
        row {
            simpleButton("/cancel")
        }
    }

internal fun unhandledCommandKeyboard(): ReplyKeyboardMarkup =
    replyKeyboard(
        resizeKeyboard = true,
        oneTimeKeyboard = true,
    ) {
        row {
            simpleButton("/commands")
        }
    }

/**
 * The share button attached to a completed round.
 *
 * The result is sent with [dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove] so the
 * move buttons disappear, then this inline keyboard is attached to the same message
 * (see `TelegramGameIo.send`).
 */
internal fun shareKeyboard(): InlineKeyboardMarkup =
    inlineKeyboard {
        row {
            +urlInlineButton("📣 Share", shareUrl())
        }
    }

/**
 * The buttons attached to an inline-mode result.
 *
 * The result can be posted in any chat, so the Play button opens the bot while the
 * Share button keeps the loop going.
 */
internal fun inlinePlayKeyboard(): InlineKeyboardMarkup =
    inlineKeyboard {
        row {
            +urlInlineButton("▶️ Play", BOT_URL)
            +urlInlineButton("📣 Share", shareUrl())
        }
    }

/**
 * Builds a Telegram share-sheet link for the bot.
 *
 * Spaces are encoded as `%20` rather than `+` so the text renders correctly in every
 * client.
 */
internal fun shareUrl(
    url: String = SHARE_START_URL,
    text: String = "I just played Rock–Paper–Scissors–Lizard–Spock. Can you beat me?",
): String = "https://t.me/share/url?url=${encode(url)}&text=${encode(text)}"

private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
