package com.el_giancar.application

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides one [Mutex] per chat so two games cannot run in the same chat at once.
 *
 * Keyed by the raw numeric chat id to keep this layer independent of Telegram types.
 *
 * Entries are reference-counted while a caller uses them and evicted when the last
 * caller releases an unlocked lock. That keeps the map from growing with every chat
 * the bot has ever seen without letting two callers hold different mutexes for the
 * same chat: acquiring and releasing both run through [ConcurrentHashMap.compute],
 * so the entry cannot be removed between a caller's pin and its use.
 */
class ChatLocks {
    internal class Entry {
        val mutex = Mutex()
        var users: Int = 0
    }

    private val entries = ConcurrentHashMap<Long, Entry>()

    /**
     * Pins the lock for [chatId]. The caller must call [ChatLock.release] exactly
     * once when done, whether or not [ChatLock.tryLock] succeeded.
     */
    fun acquire(chatId: Long): ChatLock {
        val entry = entries.compute(chatId) { _, existing ->
            (existing ?: Entry()).also { it.users++ }
        }!!
        return ChatLock(chatId, entry)
    }

    /** Whether a game currently holds the chat lock. Does not create an entry. */
    fun isBusy(chatId: Long): Boolean = entries[chatId]?.mutex?.isLocked == true

    /** Number of chats with a live entry. Exposed for tests. */
    internal fun trackedChats(): Int = entries.size

    inner class ChatLock internal constructor(
        private val chatId: Long,
        private val entry: Entry,
    ) {
        private val mutex: Mutex = entry.mutex
        private var held = false

        fun tryLock(): Boolean {
            val acquired = mutex.tryLock()
            if (acquired) {
                held = true
            }
            return acquired
        }

        /**
         * Unlocks the mutex if this handle acquired it, then drops this caller's pin.
         * The entry is evicted once no callers remain and the mutex is unlocked.
         */
        fun release() {
            if (held) {
                held = false
                mutex.unlock()
            }

            entries.computeIfPresent(chatId) { _, current ->
                if (current !== entry) return@computeIfPresent current
                current.users--
                if (current.users == 0 && !current.mutex.isLocked) null else current
            }
        }
    }
}
