@file:OptIn(PreviewFeature::class)

package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.Game
import com.el_giancar.application.ReadinessState
import com.el_giancar.application.StatsService
import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.SheldonMoveType
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMyChatMemberUpdated
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.types.chat.GroupChat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
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
    statsService: StatsService,
    connectionCheckInterval: Duration = 60.seconds,
) {
    val bot = telegramBot(token)
    val gameFlow = GameFlow(chatLocks)

    coroutineScope {
        // A long-polling bot can stay up while its Telegram session is dead (for
        // example after the token is revoked). Probe the API so /readyz reflects
        // that instead of only proving the HTTP server is alive.
        val connectionMonitor =
            launch {
                delay(connectionCheckInterval)
                while (isActive) {
                    val connected =
                        try {
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
            bot
                .buildBehaviourWithLongPolling {
                    val me = bot.getMe()
                    readiness.markReady()
                    logger.info("Connected to Telegram as {}", me.username)
                    bot.syncCommands()

                    onCommand("start", requireOnlyCommandInMessage = false) { message ->
                        val payload = startPayload(message.content.text)
                        sanitizedCampaign(payload)?.let { campaign ->
                            logger.info("Start campaign: {}", campaign)
                        }
                        replyTo(message, startMessageFor(payload), commandsKeyboard())
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
                        gameFlow.play(this, message, classicGame)?.let { result ->
                            recordResult(message, result.outcome, statsService)
                        }
                    }

                    onCommand("sheldon_game") { message ->
                        gameFlow.play(this, message, sheldonGame)?.let { result ->
                            recordResult(message, result.outcome, statsService)
                        }
                    }

                    onCommand("stats") { message ->
                        val player = message.playerOrNull()
                        val text =
                            if (player == null) {
                                statsUnavailableMessage
                            } else {
                                statsMessage(
                                    statsService.personalStats(
                                        playerId = player.id,
                                        chatId = message.chat.id.chatId.long,
                                    ),
                                )
                            }
                        replyTo(message, text, commandsKeyboard())
                    }

                    onCommand("leaderboard") { message ->
                        replyTo(
                            message,
                            globalLeaderboardMessage(statsService.globalLeaderboard()),
                            commandsKeyboard(),
                        )
                    }

                    onCommand("chat_leaderboard") { message ->
                        replyTo(
                            message,
                            chatLeaderboardMessage(
                                statsService.chatLeaderboard(message.chat.id.chatId.long),
                            ),
                            commandsKeyboard(),
                        )
                    }

                    onCommand("commands") { message ->
                        replyTo(message, commandsMenuMessage, commandsKeyboard())
                    }

                    onCommand("cancel") { message ->
                        // While a game is running, GameFlow receives the same text message
                        // and replies with cancelledMessage; replying here too would send
                        // two messages. Outside a game, explain that there is nothing to
                        // cancel instead of falling through to the unknown-command hint.
                        if (!chatLocks.isBusy(message.chat.id.chatId.long)) {
                            replyTo(message, noActiveGameMessage, commandsKeyboard())
                        }
                    }

                    onMyChatMemberUpdated { update ->
                        // Private chats also emit membership updates (for example when
                        // the user blocks the bot), so only welcome real groups.
                        if (
                            update.chat is GroupChat &&
                            isBotAddedToChat(
                                oldStatus = update.oldChatMemberState.status,
                                newStatus = update.newChatMemberState.status,
                            )
                        ) {
                            sendMessage(update.chat.id, groupWelcomeMessage)
                        }
                    }

                    onText { message ->
                        // Commands belong to the command handlers, and plain text sent
                        // during a game belongs to the GameFlow prompt loop (which
                        // re-prompts on an invalid move). Only nudge in an idle private
                        // chat, so a group conversation is never interrupted.
                        if (
                            shouldHintOnIdleText(
                                text = message.content.text,
                                chatIsBusy = chatLocks.isBusy(message.chat.id.chatId.long),
                                chatIsPrivate = message.chat is PrivateChat,
                            )
                        ) {
                            replyTo(message, idleTextMessage, commandsKeyboard())
                        }
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

/**
 * Whether an incoming text message should receive the idle hint.
 *
 * Commands are handled elsewhere, plain text during a game is the prompt loop's job,
 * and a group chat should not get a reply to every message.
 */
internal fun shouldHintOnIdleText(
    text: String,
    chatIsBusy: Boolean,
    chatIsPrivate: Boolean,
): Boolean =
    chatIsPrivate &&
        !chatIsBusy &&
        text.isNotBlank() &&
        !text.trimStart().startsWith("/")

private suspend fun recordResult(
    message: ChatContentMessage<TextContent>,
    outcome: GameOutcome,
    statsService: StatsService,
) {
    val player = message.playerOrNull() ?: return
    statsService.recordGame(
        player = player,
        chatId = message.chat.id.chatId.long,
        outcome = outcome,
    )
}
