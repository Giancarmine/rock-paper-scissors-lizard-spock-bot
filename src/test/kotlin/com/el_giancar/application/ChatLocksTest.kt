package com.el_giancar.application

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatLocksTest {
    @Test
    fun `reports a chat busy only while its game lock is held`() = runBlocking {
        val chatLocks = ChatLocks()
        val chatId = 42L

        assertFalse(chatLocks.isBusy(chatId))

        val lock = chatLocks.forChat(chatId)
        lock.lock()
        try {
            assertTrue(chatLocks.isBusy(chatId))
        } finally {
            lock.unlock()
        }

        assertFalse(chatLocks.isBusy(chatId))
    }
}
