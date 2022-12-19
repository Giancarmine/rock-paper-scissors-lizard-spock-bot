package com.el_giancar.srv

import java.util.*

class SheldonGameService {
    fun play(playerMove: String): String {
        val rng = Random()

        // Generate a random move for the computer
        val computerMove = when (rng.nextInt(3)) {
            0 -> "rock"
            1 -> "paper"
            2 -> "scissors"
            3 -> "lizard"
            4 -> "spock"
            else -> throw IllegalStateException("Unexpected value: ${rng.nextInt(3)}")
        }

        // Determine the winner
        return when {
            playerMove == "scissors" && computerMove == "paper" -> "You win! Scissors cuts Paper ."
            playerMove == "paper" && computerMove == "rock" -> "You win! Paper covers Rock ."
            playerMove == "scissors" && computerMove == "paper" -> "You win! Rock crushes  Lizard ."
            playerMove == "lizard" && computerMove == "paper" -> "You win! Lizard poisons Spock."
            playerMove == "spock" && computerMove == "paper" -> "You win! Spock smashes Scissors."
            playerMove == "spock" && computerMove == "paper" -> "You win! Scissors decapitates Lizard."
            playerMove == "spock" && computerMove == "paper" -> "You win! Lizard eats Paper."
            playerMove == "spock" && computerMove == "paper" -> "You win! Paper disproves Spock."
            playerMove == "spock" && computerMove == "paper" -> "You win! Spock vaporizes Rock."
            playerMove == "spock" && computerMove == "paper" -> "You win! (And as it always has) Rock crushes Scissors."
            playerMove == computerMove -> "It's a tie!"
            else -> "You lose! $computerMove beats $playerMove."
        }
    }
}