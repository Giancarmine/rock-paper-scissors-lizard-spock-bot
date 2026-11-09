package com.el_giancar.infrastructure.telegram

/**
 * Reads the payload from a `/start` deep link.
 *
 * Telegram sends `/start <payload>`, where the payload is limited to letters,
 * digits, `_` and `-`. In groups the command can carry the bot username
 * (`/start@Bot payload`), so the command token is compared without that suffix.
 */
internal fun startPayload(text: String): String? {
    val parts = text.trim().split(WHITESPACE)
    val command = parts.firstOrNull()?.substringBefore('@') ?: return null
    if (!command.equals("/start", ignoreCase = true)) {
        return null
    }
    return parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
}

/**
 * Keeps only characters that are safe to log and caps the length, so a crafted deep
 * link cannot flood the logs.
 */
internal fun sanitizedCampaign(payload: String?): String? =
    payload
        ?.take(MAX_CAMPAIGN_LENGTH)
        ?.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        ?.takeIf { it.isNotEmpty() }

private val WHITESPACE = Regex("\\s+")
private const val MAX_CAMPAIGN_LENGTH = 64
