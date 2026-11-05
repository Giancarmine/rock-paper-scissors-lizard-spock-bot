package com.el_giancar.domain

/**
 * A move that can be played in one of the games.
 *
 * `displayName` is the human-readable label used in bot messages and on reply
 * buttons. The enum constant name remains the stable identifier used in tests and
 * logs.
 */
interface Move {
    val displayName: String
}
