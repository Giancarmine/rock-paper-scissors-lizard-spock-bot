package com.el_giancar.domain

/**
 * A Telegram user who has finished at least one game.
 *
 * The id is Telegram's numeric user id. [displayName] is the name shown on
 * leaderboards; [username] is optional because not every Telegram account has one.
 */
data class Player(
    val id: Long,
    val displayName: String,
    val username: String? = null,
)
