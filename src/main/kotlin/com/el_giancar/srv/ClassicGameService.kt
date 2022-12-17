package com.el_giancar.srv

import java.util.*

class ClassicGameService {
    fun play(playerMove: String): String {
        val rng = Random()

        // Generate a random move for the computer
        val computerMove = when (rng.nextInt(3)) {
            0 -> "rock"
            1 -> "paper"
            2 -> "scissors"
            else -> throw IllegalStateException("Unexpected value: ${rng.nextInt(3)}")
        }

        // Determine the winner
        return when {
            playerMove == "rock" && computerMove == "scissors" -> "You win! Rock beats scissors."
            playerMove == "paper" && computerMove == "rock" -> "You win! Paper beats rock."
            playerMove == "scissors" && computerMove == "paper" -> "You win! Scissors beat paper."
            playerMove == computerMove -> "It's a tie!"
            else -> "You lose! $computerMove beats $playerMove."
        }
    }
}