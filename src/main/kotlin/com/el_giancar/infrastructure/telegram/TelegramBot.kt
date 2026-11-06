@file:OptIn(PreviewFeature::class)

package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.Game
import com.el_giancar.application.ReadinessState
import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.SheldonMoveType
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.utils.PreviewFeature
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = LoggerFactory.getLogger("com.el_giancar.infrastructure.telegram.TelegramBot")

suspend fun runTelegramBot(
    token: String,
    classicGame: Game<ClassicMoveType>,
    sheldonGame: Game<SheldonMoveType>,
    chatLocks: ChatLocks,
    readiness: ReadinessState,
    connectionCheckInterval: Duration = 60.seconds,
) {
    val bot = telegramBot(token)
    val gameFlow = GameFlow(chatLocks)

    coroutineScope {
        // A long-polling bot can stay up while its Telegram session is dead (for
        // example after the token is revoked). Probe the API so /readyz reflects
        // that instead of only proving the HTTP server is alive.
        val connectionMonitor = launch {
            delay(connectionCheckInterval)
            while (isActive) {
                val connected = try {
                    bot.getMe()
                    true
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    false
                }

                if (connected) {
                    readiness.markReady()
                } else {
                    readiness.markNotReady()
                    logger.warn("Telegram readiness check failed; /readyz will report NOT READY")
                }

                delay(connectionCheckInterval)
            }
        }

        try {
            bot.buildBehaviourWithLongPolling {
                val me = bot.getMe()
                readiness.markReady()
                logger.info("Connected to Telegram as {}", me.username)

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
        } finally {
            readiness.markNotReady()
            connectionMonitor.cancelAndJoin()
        }
    }
}
