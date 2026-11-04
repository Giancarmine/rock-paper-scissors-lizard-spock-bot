@file:OptIn(PreviewFeature::class)

package com.el_giancar.plugins

import com.el_giancar.model.ClassicMoveType
import com.el_giancar.model.SheldonMoveType
import com.el_giancar.srv.ClassicGameService
import com.el_giancar.srv.SheldonGameService
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

private const val MAX_MOVE_ATTEMPTS = 3

private val rulesMessage: String = "The rules are pretty simple which you could even memorize \uD83E\uDDE0 \n\n" +
        "- Scissors cuts Paper \n" +
        "- Paper covers Rock \n" +
        "- Rock crushes Lizard \n" +
        "- Lizard poisons Spock\n" +
        "- Spock smashes Scissors\n" +
        "- Scissors decapitates Lizard\n" +
        "- Lizard eats Paper\n" +
        "- Paper disproves Spock\n" +
        "- Spock vaporizes Rock\n" +
        "- (and as it always has) Rock crushes Scissors"

private val helpMessage: String = """
    |Available commands:
    |/classic_game - play with Rock, Paper, Scissors
    |/sheldon_game - play with Rock, Paper, Scissors, Lizard, Spock
    |/rules - show the rules
    |/commands - show the command keyboard
    |/about - about this bot
    |
    |During a game, send any command to cancel it.
""".trimMargin()

private val aboutMessage: String = """
    |I'm a small open-source Telegram bot for Rock-Paper-Scissors and
    |Rock-Paper-Scissors-Lizard-Spock.
    |
    |Built with Kotlin, Ktor and tgbotapi.
    |Source: https://github.com/Giancarmine/rock-paper-scissors-lizard-spock-bot
""".trimMargin()

suspend fun configureTelegram(token: String) {
    val bot = telegramBot(token)
    val classicGameSrv = ClassicGameService()
    val sheldonGameSrv = SheldonGameService()

    // One lock per chat prevents two game handlers from consuming the same reply.
    val chatLocks = ConcurrentHashMap<Long, Mutex>()
    fun lockFor(chatId: Long): Mutex = chatLocks.computeIfAbsent(chatId) { Mutex() }

    bot.buildBehaviourWithLongPolling {
        println(getMe())

        onCommand("start") { message ->
            reply(message, "Hi! Click at /commands to start playing \uD83D\uDD25")
            printCommandsMenu(message)
        }

        onCommand("rules") { message ->
            reply(message, rulesMessage)
            printCommandsMenu(message)
        }

        onCommand("help") { message ->
            reply(message, helpMessage)
            printCommandsMenu(message)
        }

        onCommand("about") { message ->
            reply(message, aboutMessage)
            printCommandsMenu(message)
        }

        onCommand("classic_game") { message ->
            handleGame(
                message = message,
                lock = lockFor(message.chat.id.chatId),
                prompt = "Send me your \"choose\"",
                moves = ClassicMoveType.values().toList(),
                play = { move -> classicGameSrv.play(move) }
            )
        }

        onCommand("sheldon_game") { message ->
            handleGame(
                message = message,
                lock = lockFor(message.chat.id.chatId),
                prompt = "Send me your \"choose\"",
                moves = SheldonMoveType.values().toList(),
                play = { move -> sheldonGameSrv.play(move) }
            )
        }

        onCommand("commands") { message ->
            printCommandsMenu(message)
        }

        onUnhandledCommand { message ->
            reply(
                message,
                replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                    row {
                        simpleButton("/commands")
                    }
                }
            ) {
                +"Use " + botCommand("commands") + " to get the available commands"
            }
        }
    }.join()
}

private suspend fun <T : Enum<T>> BehaviourContext.handleGame(
    message: CommonMessage<TextContent>,
    lock: Mutex,
    prompt: String,
    moves: List<T>,
    play: (T) -> String
) {
    if (!lock.tryLock()) {
        reply(message, "A game is already in progress in this chat. Finish it first.")
        return
    }

    try {
        val move = awaitMove(message, prompt, moves) ?: return
        reply(message, play(move))
        printCommandsMenu(message)
    } finally {
        lock.unlock()
    }
}

private suspend fun <T : Enum<T>> BehaviourContext.awaitMove(
    message: CommonMessage<TextContent>,
    prompt: String,
    moves: List<T>
): T? {
    var hint = prompt

    repeat(MAX_MOVE_ATTEMPTS) {
        reply(
            message,
            hint,
            replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
                moves.forEach { move ->
                    row {
                        simpleButton(move.name)
                    }
                }
            }
        )

        val playerMessage = waitTextMessage().first { it.chat.id == message.chat.id }
        val rawMove = playerMessage.content.text.trim()

        if (rawMove.startsWith("/")) {
            reply(playerMessage, "Game cancelled.")
            printCommandsMenu(playerMessage)
            return null
        }

        val move = moves.firstOrNull { it.name.equals(rawMove, ignoreCase = true) }
        if (move != null) {
            return move
        }

        hint = "Unknown move. Choose one of: ${moves.joinToString(", ") { it.name }}"
    }

    reply(message, "Too many invalid attempts. Game cancelled.")
    printCommandsMenu(message)
    return null
}

private suspend fun BehaviourContext.printCommandsMenu(message: CommonMessage<TextContent>) {
    reply(
        message,
        replyMarkup = replyKeyboard(resizeKeyboard = true, oneTimeKeyboard = true) {
            row {
                simpleButton("/classic_game")
                simpleButton("/sheldon_game")
                simpleButton("/rules")
            }
            row {
                simpleButton("/help")
                simpleButton("/commands")
                simpleButton("/about")
            }
        }
    ) {
        +"Use the buttons to start"
    }
}
