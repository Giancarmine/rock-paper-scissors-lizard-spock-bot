package com.el_giancar.application

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides one [Mutex] per chat so two games cannot run in the same chat at once.
 *
 * Keyed by the raw numeric chat id to keep this layer independent of Telegram types.
 */
class ChatLocks {
    private val locks = ConcurrentHashMap<Long, Mutex>()

    fun forChat(chatId: Long): Mutex = locks.computeIfAbsent(chatId) { Mutex() }
}
