@file:OptIn(PreviewFeature::class)

package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.Game
import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.SheldonMoveType
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.utils.PreviewFeature
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.el_giancar.infrastructure.telegram.TelegramBot")

suspend fun runTelegramBot(
    token: String,
    classicGame: Game<ClassicMoveType>,
    sheldonGame: Game<SheldonMoveType>,
    chatLocks: ChatLocks,
) {
    val bot = telegramBot(token)
    val gameFlow = GameFlow(chatLocks)

    bot.buildBehaviourWithLongPolling {
        val me = bot.getMe()
        logger.info("Connected to Telegram as @{}", me.username)

        onCommand("start") { message ->
            replyTo(message, startMessage, commandsKeyboard())
        }

        onCommand("rules") { message ->
            replyTo(message, rulesMessage, commandsKeyboard())
        }

        onCommand("help") { message ->
            replyTo(message, helpMessage, commandsKeyboard())
        }

        onCommand("about") { message ->
            replyTo(message, aboutMessage, commandsKeyboard())
        }

        onCommand("classic_game") { message ->
            gameFlow.play(this, message, classicGame)
        }

        onCommand("sheldon_game") { message ->
            gameFlow.play(this, message, sheldonGame)
        }

        onCommand("commands") { message ->
            replyTo(message, commandsMenuMessage, commandsKeyboard())
        }

        onUnhandledCommand { message ->
            replyTo(message, unknownCommandMessage, unhandledCommandKeyboard())
        }
    }.join()
}
