package com.el_giancar.application

import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.GameRecord
import com.el_giancar.domain.LeaderboardEntry
import com.el_giancar.domain.PersonalStats
import com.el_giancar.domain.Player
import com.el_giancar.domain.PlayerStats
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsServiceTest {
    @Test
    fun `is disabled without a store`() =
        runBlocking {
            val service = StatsService(store = null)

            assertFalse(service.enabled)
            assertEquals(StatsResult.Disabled, service.personalStats(1L, 10L))
            assertEquals(StatsResult.Disabled, service.globalLeaderboard())
            assertEquals(StatsResult.Disabled, service.chatLeaderboard(10L))
        }

    @Test
    fun `returns stored stats when the database works`() =
        runBlocking {
            val expected = PersonalStats(PlayerStats.empty(), PlayerStats.empty())
            val store = FakeStatsStore(personal = expected)
            val service = StatsService(store)

            assertTrue(service.enabled)
            assertEquals(StatsResult.Success(expected), service.personalStats(1L, 10L))
        }

    @Test
    fun `reports unavailable when a read fails`() =
        runBlocking {
            val service = StatsService(FakeStatsStore(failure = IllegalStateException("down")))

            assertEquals(StatsResult.Unavailable, service.personalStats(1L, 10L))
            assertEquals(StatsResult.Unavailable, service.globalLeaderboard())
            assertEquals(StatsResult.Unavailable, service.chatLeaderboard(10L))
        }

    @Test
    fun `swallows write failures so a game can still finish`() =
        runBlocking {
            val store = FakeStatsStore(failure = IllegalStateException("down"))
            val service = StatsService(store)

            service.recordGame(Player(1L, "Alice"), chatId = 10L, outcome = GameOutcome.WIN)

            assertEquals(1, store.recordAttempts)
        }

    @Test
    fun `records completed games in the store`() =
        runBlocking {
            val store = FakeStatsStore()
            val service = StatsService(store)

            service.recordGame(Player(1L, "Alice"), chatId = 10L, outcome = GameOutcome.WIN)

            assertEquals(1, store.recorded.size)
            assertEquals(10L, store.recorded.single().chatId)
            assertEquals(GameOutcome.WIN, store.recorded.single().outcome)
        }
}

private class FakeStatsStore(
    private val personal: PersonalStats = PersonalStats(PlayerStats.empty(), PlayerStats.empty()),
    private val failure: Exception? = null,
) : StatsStore {
    val recorded = mutableListOf<GameRecord>()
    var recordAttempts = 0

    override suspend fun recordGame(record: GameRecord) {
        recordAttempts++
        failure?.let { throw it }
        recorded += record
    }

    override suspend fun personalStats(
        playerId: Long,
        chatId: Long,
    ): PersonalStats {
        failure?.let { throw it }
        return personal
    }

    override suspend fun globalLeaderboard(limit: Int): List<LeaderboardEntry> {
        failure?.let { throw it }
        return emptyList()
    }

    override suspend fun chatLeaderboard(
        chatId: Long,
        limit: Int,
    ): List<LeaderboardEntry> {
        failure?.let { throw it }
        return emptyList()
    }

    override fun close() = Unit
}
