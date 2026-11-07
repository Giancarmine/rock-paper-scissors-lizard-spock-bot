package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.Player
import dev.inmo.tgbotapi.types.chat.CommonUser
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

/**
 * The Telegram user behind a message, or `null` for updates without a user (for
 * example channel posts). Only users can play, so callers skip statistics when this
 * returns `null`.
 */
internal fun ChatContentMessage<TextContent>.playerOrNull(): Player? {
    val user = (this as? FromUserMessage)?.user as? CommonUser ?: return null
    return Player(
        id = user.id.chatId.long,
        displayName = playerDisplayName(user.firstName, user.lastName, user.username?.toString()),
        username =
            user.username
                ?.toString()
                ?.removePrefix("@")
                ?.take(MAX_USERNAME_LENGTH),
    )
}

/**
 * Builds the leaderboard label for a Telegram user.
 *
 * The full name wins when present; otherwise the @username is used without the
 * leading `@`, and a generic label is the last resort.
 */
internal fun playerDisplayName(
    firstName: String?,
    lastName: String?,
    username: String?,
): String {
    val fullName =
        listOfNotNull(firstName?.trim()?.takeIf { it.isNotEmpty() }, lastName?.trim()?.takeIf { it.isNotEmpty() })
            .joinToString(" ")
    if (fullName.isNotEmpty()) {
        return fullName.take(MAX_DISPLAY_NAME_LENGTH)
    }

    val handle = username?.trim()?.removePrefix("@")?.takeIf { it.isNotEmpty() }
    return handle?.take(MAX_DISPLAY_NAME_LENGTH) ?: "Player"
}

private const val MAX_DISPLAY_NAME_LENGTH = 255
private const val MAX_USERNAME_LENGTH = 64
