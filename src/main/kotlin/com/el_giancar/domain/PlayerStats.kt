package com.el_giancar.domain

import java.time.Instant

/**
 * Aggregated results for one player in one scope (a chat or all chats).
 *
 * A win is worth one point; ties and losses are worth zero. [lastPlayed] is
 * `null` only when [games] is zero.
 */
data class PlayerStats(
    val games: Int,
    val wins: Int,
    val losses: Int,
    val ties: Int,
    val points: Int,
    val lastPlayed: Instant?,
) {
    companion object {
        fun empty(): PlayerStats =
            PlayerStats(
                games = 0,
                wins = 0,
                losses = 0,
                ties = 0,
                points = 0,
                lastPlayed = null,
            )
    }
}

/**
 * Personal statistics for the current chat and across every chat the player has
 * used.
 */
data class PersonalStats(
    val overall: PlayerStats,
    val chat: PlayerStats,
)
