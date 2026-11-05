package com.el_giancar.domain

enum class ClassicMoveType(override val displayName: String) : Move {
    ROCK("Rock"),
    PAPER("Paper"),
    SCISSORS("Scissors"),
}
