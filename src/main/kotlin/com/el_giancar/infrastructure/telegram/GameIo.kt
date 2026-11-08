package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlinx.coroutines.flow.first

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
        context.replyTo(message, text, keyboard)
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
