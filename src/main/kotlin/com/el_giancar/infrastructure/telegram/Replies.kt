package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.Message

internal suspend fun BehaviourContext.replyTo(
    message: Message,
    text: String,
    replyMarkup: KeyboardMarkup? = null,
) {
    reply(
        toChatId = message.chat.id,
        toMessageId = message.messageId,
        text = text,
        replyMarkup = replyMarkup,
    )
}
