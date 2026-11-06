package com.el_giancar.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatLocksTest {
    @Test
    fun `reports a chat busy only while its game lock is held`() {
        val chatLocks = ChatLocks()
        val chatId = 42L
        val lock = chatLocks.acquire(chatId)

        assertFalse(chatLocks.isBusy(chatId))

        assertTrue(lock.tryLock())
        assertTrue(chatLocks.isBusy(chatId))

        lock.release()

        assertFalse(chatLocks.isBusy(chatId))
    }

    @Test
    fun `evicts the entry once the last caller releases it`() {
        val chatLocks = ChatLocks()
        val chatId = 7L

        val lock = chatLocks.acquire(chatId)
        assertEquals(1, chatLocks.trackedChats())

        assertTrue(lock.tryLock())
        lock.release()

        assertEquals(0, chatLocks.trackedChats())
    }

    @Test
    fun `keeps the entry while another caller is still pinned`() {
        val chatLocks = ChatLocks()
        val chatId = 7L

        val first = chatLocks.acquire(chatId)
        val second = chatLocks.acquire(chatId)
        assertEquals(1, chatLocks.trackedChats())

        assertTrue(first.tryLock())
        // A second game cannot start while the first holds the lock.
        assertFalse(second.tryLock())
        second.release()

        assertEquals(1, chatLocks.trackedChats())
        assertTrue(chatLocks.isBusy(chatId))

        first.release()

        assertEquals(0, chatLocks.trackedChats())
        assertFalse(chatLocks.isBusy(chatId))
    }

    @Test
    fun `does not unlock a lock held by another caller`() {
        val chatLocks = ChatLocks()
        val chatId = 7L

        val holder = chatLocks.acquire(chatId)
        assertTrue(holder.tryLock())

        val contender = chatLocks.acquire(chatId)
        assertFalse(contender.tryLock())
        contender.release()

        // The contender's release must not have released the holder's mutex.
        assertTrue(chatLocks.isBusy(chatId))

        holder.release()
        assertFalse(chatLocks.isBusy(chatId))
    }
}
