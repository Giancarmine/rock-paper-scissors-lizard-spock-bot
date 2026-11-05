package com.el_giancar.application

import com.el_giancar.domain.Move

/**
 * The rules of one game variant: its legal moves and the phrase for every winning
 * relation. Losses are derived by looking the relation up in the opposite order.
 */
interface GameRules<M : Move> {
    val moves: List<M>

    fun phrase(winner: M, loser: M): String?
}
