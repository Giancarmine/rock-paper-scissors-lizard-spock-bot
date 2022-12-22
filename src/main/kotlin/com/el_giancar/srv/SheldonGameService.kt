package com.el_giancar.srv

import java.util.*

class SheldonGameService {
    fun play(playerMove: String): String {
        val rng = Random()

        // Generate a random move for the computer
        val computerMove = when (rng.nextInt(5)) {
            0 -> "rock"
            1 -> "paper"
            2 -> "scissors"
            3 -> "lizard"
            4 -> "spock"
            else -> throw IllegalStateException("Unexpected value: ${rng.nextInt(5)}")
        }

        // Determine the winner
        return when {
            playerMove == "paper" && computerMove == "rock" -> "You win! Paper covers Rock."
            playerMove == "paper" && computerMove == "spock" -> "You win! Paper disproves Spock."
            playerMove == "rock" && computerMove == "scissors" -> "You win! (And as it always has) Rock crushes Scissors."
            playerMove == "rock" && computerMove == "lizard" -> "You win! Rock crushes  Lizard."
            playerMove == "scissors" && computerMove == "paper" -> "You win! Scissors cuts Paper."
            playerMove == "scissors" && computerMove == "lizard" -> "You win! Scissors decapitates Lizard."
            playerMove == "lizard" && computerMove == "spock" -> "You win! Lizard poisons Spock."
            playerMove == "lizard" && computerMove == "paper" -> "You win! Lizard eats Paper."
            playerMove == "spock" && computerMove == "scissors" -> "You win! Spock smashes Scissors."
            playerMove == "spock" && computerMove == "rock" -> "You win! Spock vaporizes Rock."
            playerMove == computerMove -> "It's a tie!"
            else -> "You lose! $computerMove beats $playerMove."
        }
    }
}