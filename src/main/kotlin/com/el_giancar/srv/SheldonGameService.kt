package com.el_giancar.srv

import com.el_giancar.model.SheldonMoveType
import java.util.Random

class SheldonGameService(private val random: Random = Random()) {

    /**
     * Plays a round choosing the computer move randomly.
     */
    fun play(playerMove: SheldonMoveType): String = play(playerMove, randomMove())

    /**
     * Plays a deterministic round. Useful for tests and for callers that already
     * know the computer move.
     */
    fun play(playerMove: SheldonMoveType, computerMove: SheldonMoveType): String {
        val playerPhrase = winningPhrases[playerMove to computerMove]
        val computerPhrase = winningPhrases[computerMove to playerMove]

        return when {
            playerMove == computerMove -> "It's a tie!"
            playerPhrase != null -> "You win! $playerPhrase."
            computerPhrase != null -> "You lose! $computerPhrase."
            else -> error("Unhandled Sheldon moves: $playerMove vs $computerMove")
        }
    }

    private fun randomMove(): SheldonMoveType {
        val moves = SheldonMoveType.values()
        return moves[random.nextInt(moves.size)]
    }

    private companion object {
        val winningPhrases = mapOf(
            (SheldonMoveType.ROCK to SheldonMoveType.SCISSORS) to "Rock crushes Scissors",
            (SheldonMoveType.ROCK to SheldonMoveType.LIZARD) to "Rock crushes Lizard",
            (SheldonMoveType.PAPER to SheldonMoveType.ROCK) to "Paper covers Rock",
            (SheldonMoveType.PAPER to SheldonMoveType.SPOCK) to "Paper disproves Spock",
            (SheldonMoveType.SCISSORS to SheldonMoveType.PAPER) to "Scissors cuts Paper",
            (SheldonMoveType.SCISSORS to SheldonMoveType.LIZARD) to "Scissors decapitates Lizard",
            (SheldonMoveType.LIZARD to SheldonMoveType.PAPER) to "Lizard eats Paper",
            (SheldonMoveType.LIZARD to SheldonMoveType.SPOCK) to "Lizard poisons Spock",
            (SheldonMoveType.SPOCK to SheldonMoveType.ROCK) to "Spock vaporizes Rock",
            (SheldonMoveType.SPOCK to SheldonMoveType.SCISSORS) to "Spock smashes Scissors"
        )
    }
}
