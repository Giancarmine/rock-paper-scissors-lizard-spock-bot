package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.Move

/**
 * Parses a user's text into a move.
 *
 * Matching is case-insensitive, so hand-typed text (`spock`, `SPOCK`) and the reply
 * keyboard labels (`Spock`) both work.
 */
internal fun <M : Move> parseMove(moves: List<M>, input: String): M? {
    val normalized = input.trim()
    return moves.firstOrNull { move ->
        move.displayName.equals(normalized, ignoreCase = true)
    }
}
