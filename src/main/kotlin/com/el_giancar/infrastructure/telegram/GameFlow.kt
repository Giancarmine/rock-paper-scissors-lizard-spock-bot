package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.Game
import com.el_giancar.domain.Move
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.content.TextMessage
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Runs one game turn: acquires the chat lock, asks for a move, applies the rules,
 * and renders the result. Keeping this flow out of the command wiring makes the
 * Telegram adapter easier to read and keeps message formatting in one place.
 *
 * Telegram I/O goes through [GameIo], so the loop can be tested without a real bot.
 */
internal class GameFlow(
    private val chatLocks: ChatLocks,
    private val gameTimeout: Duration = 2.minutes,
    private val maxMoveAttempts: Int = 3,
    private val ioFor: (BehaviourContext, TextMessage) -> GameIo = { context, message ->
        TelegramGameIo(context, message)
    },
) {
    suspend fun <M : Move> play(
        context: BehaviourContext,
        message: TextMessage,
        game: Game<M>,
    ) {
        play(
            chatId = message.chat.id.chatId.long,
            game = game,
            io = ioFor(context, message),
        )
    }

    internal suspend fun <M : Move> play(
        chatId: Long,
        game: Game<M>,
        io: GameIo,
    ) {
        val chatLock = chatLocks.acquire(chatId)
        if (!chatLock.tryLock()) {
            chatLock.release()
            io.send(busyMessage, keyboard = null)
            return
        }

        try {
            val attempt =
                withTimeoutOrNull(gameTimeout) {
                    awaitMove(game.moves, io)
                }

            when {
                attempt == null -> io.send(timeoutMessage, commandsKeyboard())
                attempt.status == MoveAttemptStatus.CANCELLED ->
                    io.send(cancelledMessage, commandsKeyboard())
                attempt.status == MoveAttemptStatus.TOO_MANY_ATTEMPTS ->
                    io.send(tooManyAttemptsMessage, commandsKeyboard())
                else -> {
                    val move = requireNotNull(attempt.move) { "A selected attempt must carry a move" }
                    io.send(resultMessage(game.play(move)), commandsKeyboard())
                }
            }
        } finally {
            chatLock.release()
        }
    }

    private suspend fun <M : Move> awaitMove(
        moves: List<M>,
        io: GameIo,
    ): MoveAttempt<M> {
        var prompt = movePrompt(moves)

        repeat(maxMoveAttempts) {
            io.send(prompt, movesKeyboard(moves))

            val input = io.awaitText().trim()

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
