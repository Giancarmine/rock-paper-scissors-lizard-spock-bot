package com.el_giancar.domain

enum class SheldonMoveType(override val displayName: String) : Move {
    ROCK("Rock"),
    PAPER("Paper"),
    SCISSORS("Scissors"),
    LIZARD("Lizard"),
    SPOCK("Spock"),
}
