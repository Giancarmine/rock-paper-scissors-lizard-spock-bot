package com.el_giancar.application

import com.el_giancar.domain.ClassicMoveType
import java.util.Random

class ClassicGameService(private val random: Random = Random()) {

    /**
     * Plays a round choosing the computer move randomly.
     */
    fun play(playerMove: ClassicMoveType): String = play(playerMove, randomMove())

    /**
     * Plays a deterministic round. Useful for tests and for callers that already
     * know the computer move.
     */
    fun play(playerMove: ClassicMoveType, computerMove: ClassicMoveType): String {
        val playerPhrase = winningPhrases[playerMove to computerMove]
        val computerPhrase = winningPhrases[computerMove to playerMove]

        return when {
            playerMove == computerMove -> "It's a tie!"
            playerPhrase != null -> "You win! $playerPhrase."
            computerPhrase != null -> "You lose! $computerPhrase."
            else -> error("Unhandled classic moves: $playerMove vs $computerMove")
        }
    }

    private fun randomMove(): ClassicMoveType {
        val moves = ClassicMoveType.entries
        return moves[random.nextInt(moves.size)]
    }

    private companion object {
        val winningPhrases = mapOf(
            (ClassicMoveType.ROCK to ClassicMoveType.SCISSORS) to "Rock crushes Scissors",
            (ClassicMoveType.PAPER to ClassicMoveType.ROCK) to "Paper covers Rock",
            (ClassicMoveType.SCISSORS to ClassicMoveType.PAPER) to "Scissors cuts Paper"
        )
    }
}
