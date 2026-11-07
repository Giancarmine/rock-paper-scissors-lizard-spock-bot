package com.el_giancar.application

import com.el_giancar.domain.GameRecord
import com.el_giancar.domain.LeaderboardEntry
import com.el_giancar.domain.PersonalStats

/**
 * Persistence boundary for player statistics.
 *
 * Implementations may throw when the database is unavailable; [StatsService]
 * turns those failures into an "unavailable" result so gameplay never depends on
 * the database.
 */
interface StatsStore {
    suspend fun recordGame(record: GameRecord)

    suspend fun personalStats(
        playerId: Long,
        chatId: Long,
    ): PersonalStats

    suspend fun globalLeaderboard(limit: Int): List<LeaderboardEntry>

    suspend fun chatLeaderboard(
        chatId: Long,
        limit: Int,
    ): List<LeaderboardEntry>

    fun close()
}
