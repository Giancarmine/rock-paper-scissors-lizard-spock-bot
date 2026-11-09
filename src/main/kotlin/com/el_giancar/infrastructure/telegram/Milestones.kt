package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.LeaderboardEntry

/**
 * A notable score event worth announcing in a group.
 *
 * Milestones are deliberately rare: a new chat leader or a round-number point total.
 * Announcing every win would turn the chat into noise, so [detectMilestone] returns
 * at most one event per completed round.
 */
internal sealed interface Milestone {
    data class NewLeader(
        val displayName: String,
        val points: Int,
    ) : Milestone

    data class Points(
        val displayName: String,
        val points: Int,
    ) : Milestone
}

/** Point totals that are announced when a player reaches them in a chat. */
internal val pointMilestones: List<Int> = listOf(10, 25, 50, 100, 250, 500, 1000)

/**
 * Returns the milestone a completed round created, or `null`.
 *
 * [pointsBefore] is the player's chat total before the round. Only a win adds a point,
 * so a tie or a loss can never reach a new total. A new leader takes priority over a
 * point milestone because one round should produce at most one announcement.
 */
internal fun detectMilestone(
    displayName: String,
    playerId: Long,
    pointsBefore: Int,
    wonRound: Boolean,
    leaderboardBefore: List<LeaderboardEntry>,
    leaderboardAfter: List<LeaderboardEntry>,
): Milestone? {
    val pointsAfter = pointsBefore + if (wonRound) 1 else 0

    val rankBefore = leaderboardBefore.find { it.playerId == playerId }?.rank
    val rankAfter = leaderboardAfter.find { it.playerId == playerId }?.rank
    if (rankAfter == 1 && rankBefore != 1 && pointsAfter > 0) {
        return Milestone.NewLeader(displayName, pointsAfter)
    }

    val reached = pointMilestones.firstOrNull { it > pointsBefore && it <= pointsAfter }
    return reached?.let { Milestone.Points(displayName, it) }
}
