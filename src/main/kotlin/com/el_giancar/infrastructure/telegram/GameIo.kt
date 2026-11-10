package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.el_giancar.infrastructure.telegram.GameIo")

/**
 * The Telegram operations one game needs.
 *
 * Keeping this behind an interface lets [GameFlow] run without a real
 * [BehaviourContext], so the prompt loop, retries, timeout and cancellation can be
 * unit-tested with plain strings.
 */
internal interface GameIo {
    suspend fun send(
        text: String,
        keyboard: KeyboardMarkup?,
    )

    /**
     * Waits for the next text message from the player who started the game in this
     * chat and returns its text. Text from other members of a group is ignored so it
     * cannot be played or credited to the wrong player.
     */
    suspend fun awaitText(): String
}

internal class TelegramGameIo(
    private val context: BehaviourContext,
    private val message: TextMessage,
) : GameIo {
    private val originChatId: Long = message.chat.id.chatId.long
    private val originUserId: Long? = message.userIdOrNull()

    override suspend fun send(
        text: String,
        keyboard: KeyboardMarkup?,
    ) {
        val inline = inlineKeyboardToAttach(keyboard)
        if (inline != null) {
            // A message can carry either a reply keyboard or an inline keyboard, not
            // both, and an inline keyboard does not dismiss the reply keyboard left by
            // the move prompt. Send the text with ReplyKeyboardRemove first, then attach
            // the inline buttons to the same message so the move buttons disappear.
            val sent = context.replyTo(message, text, ReplyKeyboardRemove())
            try {
                context.editMessageReplyMarkup(sent, replyMarkup = inline)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // The result is already delivered; a missing share button is harmless.
                logger.warn(
                    "Could not attach the result keyboard ({})",
                    exception.javaClass.simpleName,
                )
            }
        } else {
            context.replyTo(message, text, keyboard)
        }
    }

    override suspend fun awaitText(): String =
        context
            .waitTextMessage()
            .first { incoming ->
                isMoveFromOriginatingPlayer(
                    originChatId = originChatId,
                    originUserId = originUserId,
                    incomingChatId = incoming.chat.id.chatId.long,
                    incomingUserId = incoming.userIdOrNull(),
                )
            }.content.text
}

/**
 * The inline keyboard to attach after the reply keyboard is dismissed, or `null`.
 *
 * Only inline keyboards need the remove-then-edit treatment; reply keyboards (and no
 * keyboard) are sent as the message's markup directly.
 */
internal fun inlineKeyboardToAttach(keyboard: KeyboardMarkup?): InlineKeyboardMarkup? = keyboard as? InlineKeyboardMarkup

/**
 * Whether a text update may be used as the move for a game.
 *
 * The chat check keeps updates from other conversations out, and the user check keeps
 * other members of a group from playing (or cancelling) someone else's game. A game
 * without an identifiable originating user (for example an anonymous channel post)
 * accepts no moves and times out.
 */
internal fun isMoveFromOriginatingPlayer(
    originChatId: Long,
    originUserId: Long?,
    incomingChatId: Long,
    incomingUserId: Long?,
): Boolean =
    originUserId != null &&
        originChatId == incomingChatId &&
        originUserId == incomingUserId

private fun TextMessage.userIdOrNull(): Long? =
    (this as? FromUserMessage)
        ?.user
        ?.id
        ?.chatId
        ?.long
