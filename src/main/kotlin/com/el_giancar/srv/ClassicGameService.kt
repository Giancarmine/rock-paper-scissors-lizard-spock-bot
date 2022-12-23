package com.el_giancar.srv

import com.el_giancar.model.ClassicMoveType
import java.util.*

class ClassicGameService {
    fun play(playerMove: String): String {
        val rng = Random()

        // Generate a random move for the computer
        val computerMove = when (rng.nextInt(3)) {
            0 -> ClassicMoveType.ROCK.name
            1 -> ClassicMoveType.PAPER.name
            2 -> ClassicMoveType.SCISSORS.name
            else -> throw IllegalStateException("Unexpected value: ${rng.nextInt(3)}")
        }

        // Determine the winner
        return when {
            playerMove.equals(ClassicMoveType.ROCK.name, ignoreCase = true) && computerMove.equals(ClassicMoveType.SCISSORS.name, ignoreCase = true) -> "You win! Rock beats scissors."
            playerMove.equals(ClassicMoveType.PAPER.name, ignoreCase = true) && computerMove.equals(ClassicMoveType.ROCK.name, ignoreCase = true) -> "You win! Paper beats rock."
            playerMove.equals(ClassicMoveType.SCISSORS.name, ignoreCase = true) && computerMove.equals(ClassicMoveType.PAPER.name, ignoreCase = true) -> "You win! Scissors beat paper."
            playerMove == computerMove -> "It's a tie!"
            else -> "You lose! $computerMove beats $playerMove."
        }
    }
}