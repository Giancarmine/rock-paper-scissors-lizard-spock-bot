package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.Game
import com.el_giancar.domain.Move
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Runs one game turn: acquires the chat lock, asks for a move, applies the rules,
 * and renders the result. Keeping this flow out of the command wiring makes the
 * Telegram adapter easier to read and keeps message formatting in one place.
 */
internal class GameFlow(
    private val chatLocks: ChatLocks,
    private val gameTimeout: Duration = 2.minutes,
    private val maxMoveAttempts: Int = 3,
) {
    suspend fun <M : Move> play(
        context: BehaviourContext,
        message: TextMessage,
        game: Game<M>,
    ) {
        val lock = chatLocks.forChat(message.chat.id.chatId.long)
        if (!lock.tryLock()) {
            context.replyTo(message, busyMessage)
            return
        }

        try {
            val attempt = withTimeoutOrNull(gameTimeout) {
                context.awaitMove(message, game.moves)
            }

            when {
                attempt == null -> context.finish(message, timeoutMessage)
                attempt.status == MoveAttemptStatus.CANCELLED ->
                    context.finish(message, cancelledMessage)
                attempt.status == MoveAttemptStatus.TOO_MANY_ATTEMPTS ->
                    context.finish(message, tooManyAttemptsMessage)
                else -> {
                    val move = requireNotNull(attempt.move) { "A selected attempt must carry a move" }
                    context.replyTo(message, resultMessage(game.play(move)), commandsKeyboard())
                }
            }
        } finally {
            lock.unlock()
        }
    }

    private suspend fun <M : Move> BehaviourContext.awaitMove(
        message: TextMessage,
        moves: List<M>,
    ): MoveAttempt<M> {
        var prompt = movePrompt(moves)

        repeat(maxMoveAttempts) {
            replyTo(message, prompt, movesKeyboard(moves))

            val playerMessage = waitTextMessage().first { it.chat.id == message.chat.id }
            val input = playerMessage.content.text.trim()

            if (input.startsWith("/")) {
                return MoveAttempt(MoveAttemptStatus.CANCELLED)
            }

            val move = parseMove(moves, input)
            if (move != null) {
                return MoveAttempt(MoveAttemptStatus.SELECTED, move)
            }

            prompt = unknownMoveMessage(moves, input)
        }

        return MoveAttempt(MoveAttemptStatus.TOO_MANY_ATTEMPTS)
    }

    private suspend fun BehaviourContext.finish(message: TextMessage, text: String) {
        replyTo(message, text, commandsKeyboard())
    }
}

private enum class MoveAttemptStatus {
    SELECTED,
    CANCELLED,
    TOO_MANY_ATTEMPTS,
}

private data class MoveAttempt<out M : Move>(
    val status: MoveAttemptStatus,
    val move: M? = null,
)
