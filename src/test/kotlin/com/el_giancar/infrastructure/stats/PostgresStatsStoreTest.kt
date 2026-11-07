package com.el_giancar.infrastructure.stats

import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.GameRecord
import com.el_giancar.domain.Player
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostgresStatsStoreTest {
    @Test
    fun `records personal stats for the chat and overall`() =
        runBlocking {
            withStore { store ->
                val alice = Player(id = 1L, displayName = "Alice")
                val first = Instant.parse("2026-11-08T10:00:00Z")
                val second = Instant.parse("2026-11-08T11:30:00Z")

                store.recordGame(GameRecord(alice, chatId = 10L, outcome = GameOutcome.WIN, playedAt = first))
                store.recordGame(GameRecord(alice, chatId = 10L, outcome = GameOutcome.LOSE, playedAt = second))
                store.recordGame(GameRecord(alice, chatId = 20L, outcome = GameOutcome.TIE, playedAt = second))

                val stats = store.personalStats(playerId = 1L, chatId = 10L)

                assertEquals(2, stats.chat.games)
                assertEquals(1, stats.chat.wins)
                assertEquals(1, stats.chat.losses)
                assertEquals(0, stats.chat.ties)
                assertEquals(1, stats.chat.points)
                assertEquals(second, stats.chat.lastPlayed)

                assertEquals(3, stats.overall.games)
                assertEquals(1, stats.overall.wins)
                assertEquals(1, stats.overall.losses)
                assertEquals(1, stats.overall.ties)
                assertEquals(1, stats.overall.points)
                assertEquals(second, stats.overall.lastPlayed)
            }
        }

    @Test
    fun `returns empty stats for a player who has not played`() =
        runBlocking {
            withStore { store ->
                val stats = store.personalStats(playerId = 99L, chatId = 10L)

                assertEquals(0, stats.overall.games)
                assertEquals(0, stats.chat.games)
                assertNull(stats.overall.lastPlayed)
            }
        }

    @Test
    fun `global leaderboard sums chats and gives tied scores the same rank`() =
        runBlocking {
            withStore { store ->
                val alice = Player(1L, "Alice")
                val bob = Player(2L, "Bob")
                val carol = Player(3L, "Carol")
                val dave = Player(4L, "Dave")

                store.recordGame(GameRecord(alice, 10L, GameOutcome.WIN))
                store.recordGame(GameRecord(alice, 20L, GameOutcome.WIN))
                store.recordGame(GameRecord(bob, 10L, GameOutcome.WIN))
                store.recordGame(GameRecord(bob, 20L, GameOutcome.LOSE))
                store.recordGame(GameRecord(carol, 30L, GameOutcome.WIN))
                store.recordGame(GameRecord(dave, 10L, GameOutcome.TIE))

                val entries = store.globalLeaderboard(limit = 10)

                assertEquals(listOf("Alice", "Bob", "Carol", "Dave"), entries.map { it.displayName })
                assertEquals(listOf(1, 2, 2, 4), entries.map { it.rank })
                assertEquals(listOf(2, 1, 1, 0), entries.map { it.points })
                assertEquals(listOf(2, 2, 1, 1), entries.map { it.games })
            }
        }

    @Test
    fun `chat leaderboard only includes players from that chat`() =
        runBlocking {
            withStore { store ->
                store.recordGame(GameRecord(Player(1L, "Alice"), chatId = 10L, outcome = GameOutcome.WIN))
                store.recordGame(GameRecord(Player(2L, "Bob"), chatId = 10L, outcome = GameOutcome.WIN))
                store.recordGame(GameRecord(Player(3L, "Carol"), chatId = 20L, outcome = GameOutcome.WIN))

                val entries = store.chatLeaderboard(chatId = 10L, limit = 10)

                assertEquals(setOf("Alice", "Bob"), entries.map { it.displayName }.toSet())
                assertTrue(entries.all { it.rank == 1 }, "Both players have one point: $entries")
                assertEquals(emptyList(), store.chatLeaderboard(chatId = 30L, limit = 10))
            }
        }

    @Test
    fun `keeps the latest display name for a returning player`() =
        runBlocking {
            withStore { store ->
                store.recordGame(GameRecord(Player(1L, "Alice"), chatId = 10L, outcome = GameOutcome.WIN))
                store.recordGame(
                    GameRecord(
                        Player(1L, "Alice Cooper", username = "alice"),
                        chatId = 20L,
                        outcome = GameOutcome.WIN,
                    ),
                )

                val entries = store.globalLeaderboard(limit = 10)

                assertEquals(1, entries.size)
                assertEquals("Alice Cooper", entries.single().displayName)
                assertEquals(2, entries.single().games)
            }
        }

    @Test
    fun `respects the leaderboard limit`() =
        runBlocking {
            withStore { store ->
                repeat(3) { index ->
                    store.recordGame(
                        GameRecord(
                            Player(index.toLong() + 1, "Player ${index + 1}"),
                            chatId = 10L,
                            outcome = GameOutcome.WIN,
                        ),
                    )
                }

                assertEquals(2, store.globalLeaderboard(limit = 2).size)
                assertEquals(2, store.chatLeaderboard(chatId = 10L, limit = 2).size)
            }
        }

    @Test
    fun `converts hosted PostgreSQL URLs into JDBC settings`() {
        val settings =
            jdbcSettings(
                "postgresql://alice:p%40ss@db.example.com:5432/neondb?sslmode=require",
            )

        assertEquals("jdbc:postgresql://db.example.com:5432/neondb?sslmode=require", settings.url)
        assertEquals("alice", settings.user)
        assertEquals("p@ss", settings.password)
    }

    @Test
    fun `accepts postgres scheme and explicit credentials`() {
        val settings =
            jdbcSettings(
                rawUrl = "postgres://db.example.com/neondb",
                user = "bob",
                password = "secret",
            )

        assertEquals("jdbc:postgresql://db.example.com/neondb", settings.url)
        assertEquals("bob", settings.user)
        assertEquals("secret", settings.password)
    }

    @Test
    fun `leaves JDBC URLs without embedded credentials unchanged`() {
        val settings = jdbcSettings("jdbc:postgresql://db.example.com:5432/neondb")

        assertEquals("jdbc:postgresql://db.example.com:5432/neondb", settings.url)
        assertNull(settings.user)
        assertNull(settings.password)
    }

    @Test
    fun `moves query-string credentials out of the JDBC URL`() {
        val settings =
            jdbcSettings(
                "jdbc:postgresql://db.example.com:5432/neondb?user=alice&password=s%40cret&sslmode=require",
            )

        assertEquals("jdbc:postgresql://db.example.com:5432/neondb?sslmode=require", settings.url)
        assertEquals("alice", settings.user)
        assertEquals("s@cret", settings.password)
    }

    @Test
    fun `keeps non-PostgreSQL URLs unchanged for tests and local tools`() {
        val url = "jdbc:h2:mem:test;MODE=PostgreSQL"

        assertEquals(url, jdbcSettings(url).url)
    }

    private suspend fun withStore(block: suspend (PostgresStatsStore) -> Unit) {
        val store =
            PostgresStatsStore.create(
                rawUrl = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            )
        try {
            store.initialize()
            block(store)
        } finally {
            store.close()
        }
    }
}
