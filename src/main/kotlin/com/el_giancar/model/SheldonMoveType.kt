package com.el_giancar.model

enum class SheldonMoveType(s: String) {
    PAPER("paper"),
    ROCK("rock"),
    SCISSORS("scissors"),
    LIZARD("lizard"),
    SPOCK("spock")
}

inline fun <reified T : Enum<T>> getNames() = enumValues<T>().map { it.name }