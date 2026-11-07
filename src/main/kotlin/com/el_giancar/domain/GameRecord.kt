package com.el_giancar.domain

import java.time.Instant

/**
 * A completed round that should be counted in the statistics.
 *
 * Only finished rounds are recorded; cancellations, timeouts, and invalid attempts
 * never reach this type.
 */
data class GameRecord(
    val player: Player,
    val chatId: Long,
    val outcome: GameOutcome,
    val playedAt: Instant = Instant.now(),
)
