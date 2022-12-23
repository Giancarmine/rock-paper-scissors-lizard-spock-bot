package com.el_giancar.srv

import com.el_giancar.model.SheldonMoveType
import java.util.*

class SheldonGameService {
    fun play(playerMove: String): String {
        val rng = Random()

        // Generate a random move for the computer
        val computerMove = when (rng.nextInt(5)) {
            0 -> SheldonMoveType.ROCK.name
            1 -> SheldonMoveType.PAPER.name
            2 -> SheldonMoveType.SCISSORS.name
            3 -> SheldonMoveType.LIZARD.name
            4 -> SheldonMoveType.SPOCK.name
            else -> throw IllegalStateException("Unexpected value: ${rng.nextInt(5)}")
        }

        // Determine the winner
        return when {
            playerMove.equals(SheldonMoveType.PAPER.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.ROCK.name, ignoreCase = true) -> "You win! Paper covers Rock."
            playerMove.equals(SheldonMoveType.PAPER.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.SPOCK.name, ignoreCase = true) -> "You win! Paper disproves Spock."
            playerMove.equals(SheldonMoveType.ROCK.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.SCISSORS.name, ignoreCase = true) -> "You win! (And as it always has) Rock crushes Scissors."
            playerMove.equals(SheldonMoveType.ROCK.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.LIZARD.name, ignoreCase = true) -> "You win! Rock crushes  Lizard."
            playerMove.equals(SheldonMoveType.SCISSORS.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.PAPER.name, ignoreCase = true) -> "You win! Scissors cuts Paper."
            playerMove.equals(SheldonMoveType.SCISSORS.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.LIZARD.name, ignoreCase = true) -> "You win! Scissors decapitates Lizard."
            playerMove.equals(SheldonMoveType.LIZARD.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.SPOCK.name, ignoreCase = true) -> "You win! Lizard poisons Spock."
            playerMove.equals(SheldonMoveType.LIZARD.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.PAPER.name, ignoreCase = true) -> "You win! Lizard eats Paper."
            playerMove.equals(SheldonMoveType.SPOCK.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.SCISSORS.name, ignoreCase = true) -> "You win! Spock smashes Scissors."
            playerMove.equals(SheldonMoveType.SPOCK.name, ignoreCase = true) && computerMove.equals(SheldonMoveType.ROCK.name, ignoreCase = true) -> "You win! Spock vaporizes Rock."
            playerMove.equals(computerMove, ignoreCase = true) -> "It's a tie!"
            else -> "You lose! $computerMove beats $playerMove."
        }
    }
}