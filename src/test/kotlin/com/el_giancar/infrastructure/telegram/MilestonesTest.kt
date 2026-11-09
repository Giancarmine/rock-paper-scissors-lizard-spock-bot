package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.LeaderboardEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MilestonesTest {
    @Test
    fun `announces a new chat leader`() {
        val before =
            listOf(
                entry(playerId = 2L, points = 5, rank = 1),
                entry(playerId = 1L, points = 4, rank = 2),
            )
        val after =
            listOf(
                entry(playerId = 1L, points = 5, rank = 1),
                entry(playerId = 2L, points = 5, rank = 2),
            )

        assertEquals(
            Milestone.NewLeader("Alice", points = 5),
            milestone(
                playerId = 1L,
                pointsBefore = 4,
                wonRound = true,
                before = before,
                after = after,
            ),
        )
    }

    @Test
    fun `does not announce a leader who was already first`() {
        val before = listOf(entry(playerId = 1L, points = 5, rank = 1))
        val after = listOf(entry(playerId = 1L, points = 6, rank = 1))

        assertNull(
            milestone(
                playerId = 1L,
                pointsBefore = 5,
                wonRound = true,
                before = before,
                after = after,
            ),
        )
    }

    @Test
    fun `announces a point milestone`() {
        val before = listOf(entry(playerId = 1L, points = 9, rank = 1))
        val after = listOf(entry(playerId = 1L, points = 10, rank = 1))

        assertEquals(
            Milestone.Points("Alice", points = 10),
            milestone(
                playerId = 1L,
                pointsBefore = 9,
                wonRound = true,
                before = before,
                after = after,
            ),
        )
    }

    @Test
    fun `does not announce a milestone after a loss or a tie`() {
        val before = listOf(entry(playerId = 1L, points = 9, rank = 1))
        val after = listOf(entry(playerId = 1L, points = 9, rank = 1))

        assertNull(
            milestone(
                playerId = 1L,
                pointsBefore = 9,
                wonRound = false,
                before = before,
                after = after,
            ),
        )
    }

    @Test
    fun `prefers a new leader over a point milestone`() {
        val before =
            listOf(
                entry(playerId = 2L, points = 10, rank = 1),
                entry(playerId = 1L, points = 9, rank = 2),
            )
        val after =
            listOf(
                entry(playerId = 1L, points = 10, rank = 1),
                entry(playerId = 2L, points = 10, rank = 1),
            )

        assertEquals(
            Milestone.NewLeader("Alice", points = 10),
            milestone(
                playerId = 1L,
                pointsBefore = 9,
                wonRound = true,
                before = before,
                after = after,
            ),
        )
    }

    @Test
    fun `does not announce totals below the first threshold`() {
        val before = listOf(entry(playerId = 1L, points = 4, rank = 1))
        val after = listOf(entry(playerId = 1L, points = 5, rank = 1))

        assertNull(
            milestone(
                playerId = 1L,
                pointsBefore = 4,
                wonRound = true,
                before = before,
                after = after,
            ),
        )
    }

    @Test
    fun `does not announce a leader with zero points`() {
        val before = emptyList<LeaderboardEntry>()
        val after = listOf(entry(playerId = 1L, points = 0, rank = 1))

        assertNull(
            milestone(
                playerId = 1L,
                pointsBefore = 0,
                wonRound = false,
                before = before,
                after = after,
            ),
        )
    }

    @Test
    fun `ignores totals that are not exact milestones`() {
        val before = listOf(entry(playerId = 1L, points = 10, rank = 1))
        val after = listOf(entry(playerId = 1L, points = 11, rank = 1))

        assertNull(
            milestone(
                playerId = 1L,
                pointsBefore = 10,
                wonRound = true,
                before = before,
                after = after,
            ),
        )
    }

    private fun milestone(
        playerId: Long,
        pointsBefore: Int,
        wonRound: Boolean,
        before: List<LeaderboardEntry>,
        after: List<LeaderboardEntry>,
    ): Milestone? =
        detectMilestone(
            displayName = "Alice",
            playerId = playerId,
            pointsBefore = pointsBefore,
            wonRound = wonRound,
            leaderboardBefore = before,
            leaderboardAfter = after,
        )

    private fun entry(
        playerId: Long,
        points: Int,
        rank: Int,
    ): LeaderboardEntry =
        LeaderboardEntry(
            rank = rank,
            playerId = playerId,
            displayName = "Player $playerId",
            points = points,
            games = points,
            wins = points,
        )
}
