package com.el_giancar.infrastructure.telegram

/**
 * Public bot identity used in share links.
 *
 * Telegram usernames are stable, and building the link from a constant keeps the
 * share button independent of the Telegram API. Keep this in sync with the README
 * and the BotFather username.
 */
internal const val BOT_USERNAME = "RockPaperScissorsLizardSpockBot"

internal const val BOT_URL = "https://t.me/$BOT_USERNAME"

/** Deep link that marks a share-driven start so `/start` can greet the new player. */
internal const val SHARE_START_URL = "$BOT_URL?start=share"
