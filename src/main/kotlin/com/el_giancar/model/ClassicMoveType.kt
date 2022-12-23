package com.el_giancar.model

enum class ClassicMoveType {
    PAPER,
    ROCK,
    SCISSORS
}

inline fun <reified T : Enum<T>> getNames() = enumValues<T>().map { it.name }