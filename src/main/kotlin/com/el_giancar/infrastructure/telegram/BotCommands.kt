package com.el_giancar.infrastructure.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.types.BotCommand
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory

/**
 * The `/` command menu.
 *
 * The bot publishes this list at startup, so the menu always matches the deployed
 * code. The profile text (name and descriptions) stays in the metadata workflow
 * because it is copy that can change without a deploy.
 */
internal val botCommands: List<BotCommand> =
    listOf(
        BotCommand("start", "Start the bot and show the keyboard"),
        BotCommand("classic_game", "Play Rock, Paper, Scissors"),
        BotCommand("sheldon_game", "Play the 5-move variant"),
        BotCommand("rules", "Show the rules"),
        BotCommand("stats", "Your score"),
        BotCommand("leaderboard", "Global top 10"),
        BotCommand("chat_leaderboard", "Top 10 in this chat"),
        BotCommand("help", "List the commands"),
        BotCommand("about", "About the bot and its source"),
    )

/**
 * Publishes [botCommands] to Telegram.
 *
 * A failure must not stop the bot: an outdated menu is better than no bot.
 * Cancellation is rethrown so shutdown stays clean.
 */
internal suspend fun TelegramBot.syncCommands() {
    try {
        setMyCommands(botCommands)
        logger.info("Synchronized {} commands with Telegram", botCommands.size)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        logger.warn("Could not synchronize the command menu ({})", exception.javaClass.simpleName)
    }
}

private val logger = LoggerFactory.getLogger("com.el_giancar.infrastructure.telegram.BotCommands")
