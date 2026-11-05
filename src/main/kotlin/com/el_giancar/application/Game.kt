package com.el_giancar.application

import com.el_giancar.domain.Move
import com.el_giancar.domain.RoundResult

/** A playable game variant. */
interface Game<M : Move> {
    val moves: List<M>

    /** Plays a round, choosing the opponent's move randomly. */
    fun play(playerMove: M): RoundResult<M>
}
