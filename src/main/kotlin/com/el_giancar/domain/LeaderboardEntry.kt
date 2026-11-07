package com.el_giancar.domain

/**
 * One row of a leaderboard.
 *
 * [rank] uses competition ranking: players with the same number of [points] share
 * a rank and the next distinct score skips the occupied places (1, 1, 3, ...).
 */
data class LeaderboardEntry(
    val rank: Int,
    val playerId: Long,
    val displayName: String,
    val points: Int,
    val games: Int,
    val wins: Int,
)
