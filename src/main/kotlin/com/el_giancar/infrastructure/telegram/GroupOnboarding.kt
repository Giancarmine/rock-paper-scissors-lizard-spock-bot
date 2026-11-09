package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.types.chat.member.ChatMember

/**
 * Whether a `my_chat_member` update means the bot was just added to a chat.
 *
 * Telegram also sends this update for promotions, restrictions and a user blocking
 * the bot. Only the transition from not being in the chat to being a member should
 * trigger the welcome message.
 */
internal fun isBotAddedToChat(
    oldStatus: ChatMember.Status,
    newStatus: ChatMember.Status,
): Boolean = oldStatus in INACTIVE_STATUSES && newStatus in ACTIVE_STATUSES

private val INACTIVE_STATUSES = setOf(ChatMember.Status.Left, ChatMember.Status.Kicked)

private val ACTIVE_STATUSES =
    setOf(
        ChatMember.Status.Member,
        ChatMember.Status.Administrator,
        ChatMember.Status.Creator,
    )
