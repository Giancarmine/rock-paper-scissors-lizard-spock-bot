package com.el_giancar.srv

import java.util.*

class GameService {
    fun play(playerMove: String) {
        val rng = Random()

        // Generate a random move for the computer
        val computerMove = when (rng.nextInt(3)) {
            0 -> "rock"
            1 -> "paper"
            2 -> "scissors"
            else -> throw IllegalStateException("Unexpected value: ${rng.nextInt(3)}")
        }


        // Determine the winner
        when {
            playerMove == "rock" && computerMove == "scissors" -> println("You win! Rock beats scissors.")
            playerMove == "paper" && computerMove == "rock" -> println("You win! Paper beats rock.")
            playerMove == "scissors" && computerMove == "paper" -> println("You win! Scissors beat paper.")
            playerMove == computerMove -> println("It's a tie!")
            else -> println("You lose! $computerMove beats $playerMove.")
        }
    }

}