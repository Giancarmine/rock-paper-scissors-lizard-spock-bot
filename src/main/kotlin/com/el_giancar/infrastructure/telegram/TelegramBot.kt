@file:OptIn(PreviewFeature::class)

package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.ClassicGameService
import com.el_giancar.application.SheldonGameService
import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.SheldonMoveType
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.botCommand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

private const val MAX_MOVE_ATTEMPTS = 3

suspend fun runTelegramBot(
    token: String,
    classicGameService: ClassicGameService,
    sheldonGameService: SheldonGameService,
    chatLocks: ChatLocks,
) {
    val bot = telegramBot(token)

    bot.buildBehaviourWithLongPolling {
        println(getMe())

        onCommand("start") { message ->
            replyTo(message, "Hi! Click at /commands to start playing \uD83D\uDD25")
            printCommandsMenu(message)
        }

        onCommand("rules") { message ->
            replyTo(message, rulesMessage)
            printCommandsMenu(message)
        }

        onCommand("help") { message ->
            replyTo(message, helpMessage)
            printCommandsMenu(message)
        }

        onCommand("about") { message ->
            replyTo(message, aboutMessage)
            printCommandsMenu(message)
        }

        onCommand("classic_game") { message ->
            handleGame(
                message = message,
                lock = chatLocks.forChat(message.chat.id.chatId.long),
                prompt = "Send me your \"choose\"",
                moves = ClassicMoveType.entries,
                play = { move -> classicGameService.play(move) }
            )
        }

        onCommand("sheldon_game") { message ->
            handleGame(
                message = message,
                lock = chatLocks.forChat(message.chat.id.chatId.long),
                prompt = "Send me your \"choose\"",
                moves = SheldonMoveType.entries,
                play = { move -> sheldonGameService.play(move) }
            )
        }

        onCommand("commands") { message ->
            printCommandsMenu(message)
        }

        onUnhandledCommand { message ->
            reply(
                toChatId = message.chat.id,
                toMessageId = message.messageId,
                replyMarkup = unhandledCommandKeyboard(),
            ) {
                +"Use " + botCommand("commands") + " to get the available commands"
            }
        }
    }.join()
}

private suspend fun TelegramBot.replyTo(
    message: Message,
    text: String,
    replyMarkup: KeyboardMarkup? = null,
) {
    reply(
        toChatId = message.chat.id,
        toMessageId = message.messageId,
        text = text,
        replyMarkup = replyMarkup,
    )
}

private suspend fun <T : Enum<T>> BehaviourContext.handleGame(
    message: TextMessage,
    lock: Mutex,
    prompt: String,
    moves: List<T>,
    play: (T) -> String,
) {
    if (!lock.tryLock()) {
        replyTo(message, "A game is already in progress in this chat. Finish it first.")
        return
    }

    try {
        val move = awaitMove(message, prompt, moves) ?: return
        replyTo(message, play(move))
        printCommandsMenu(message)
    } finally {
        lock.unlock()
    }
}

private suspend fun <T : Enum<T>> BehaviourContext.awaitMove(
    message: TextMessage,
    prompt: String,
    moves: List<T>,
): T? {
    var hint = prompt

    repeat(MAX_MOVE_ATTEMPTS) {
        replyTo(message, hint, movesKeyboard(moves))

        val playerMessage = waitTextMessage().first { it.chat.id == message.chat.id }
        val rawMove = playerMessage.content.text.trim()

        if (rawMove.startsWith("/")) {
            replyTo(playerMessage, "Game cancelled.")
            printCommandsMenu(playerMessage)
            return null
        }

        val move = moves.firstOrNull { it.name.equals(rawMove, ignoreCase = true) }
        if (move != null) {
            return move
        }

        hint = "Unknown move. Choose one of: ${moves.joinToString(", ") { it.name }}"
    }

    replyTo(message, "Too many invalid attempts. Game cancelled.")
    printCommandsMenu(message)
    return null
}

private suspend fun BehaviourContext.printCommandsMenu(message: Message) {
    replyTo(message, "Use the buttons to start", commandsKeyboard())
}
