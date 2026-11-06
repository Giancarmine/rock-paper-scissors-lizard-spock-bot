package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
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

    /** Waits for the next text message from the same chat and returns its text. */
    suspend fun awaitText(): String
}

internal class TelegramGameIo(
    private val context: BehaviourContext,
    private val message: TextMessage,
) : GameIo {
    override suspend fun send(
        text: String,
        keyboard: KeyboardMarkup?,
    ) {
        context.replyTo(message, text, keyboard)
    }

    override suspend fun awaitText(): String =
        context
            .waitTextMessage()
            .first { it.chat.id == message.chat.id }
            .content.text
}
