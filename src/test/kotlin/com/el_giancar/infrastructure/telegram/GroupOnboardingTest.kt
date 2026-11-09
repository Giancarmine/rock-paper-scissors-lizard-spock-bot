package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.types.chat.member.ChatMember
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupOnboardingTest {
    @Test
    fun `welcomes the bot when it joins from outside the chat`() {
        assertTrue(isBotAddedToChat(ChatMember.Status.Left, ChatMember.Status.Member))
        assertTrue(isBotAddedToChat(ChatMember.Status.Kicked, ChatMember.Status.Member))
        assertTrue(isBotAddedToChat(ChatMember.Status.Left, ChatMember.Status.Administrator))
        assertTrue(isBotAddedToChat(ChatMember.Status.Kicked, ChatMember.Status.Creator))
    }

    @Test
    fun `does not welcome on promotions or leaving`() {
        assertFalse(isBotAddedToChat(ChatMember.Status.Member, ChatMember.Status.Administrator))
        assertFalse(isBotAddedToChat(ChatMember.Status.Administrator, ChatMember.Status.Creator))
        assertFalse(isBotAddedToChat(ChatMember.Status.Member, ChatMember.Status.Left))
        assertFalse(isBotAddedToChat(ChatMember.Status.Member, ChatMember.Status.Kicked))
        assertFalse(isBotAddedToChat(ChatMember.Status.Left, ChatMember.Status.Left))
    }
}
