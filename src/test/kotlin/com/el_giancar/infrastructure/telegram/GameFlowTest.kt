package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.Game
import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.RoundResult
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class GameFlowTest {
    private val game = FixedClassicGame()

    @Test
    fun `plays a valid move and sends the prompt and the result`() =
        runBlocking {
            val io = FakeGameIo("  rOcK ")

            GameFlow(ChatLocks()).play(chatId = 1L, game = game, io = io)

            assertEquals(2, io.sent.size)
            assertTrue(io.sent[0].contains("Your move!"), io.sent[0])
            assertEquals(
                resultMessage(RoundResult(ClassicMoveType.ROCK, ClassicMoveType.SCISSORS, GameOutcome.WIN, "Rock crushes Scissors")),
                io.sent[1],
            )
        }

    @Test
    fun `re-prompts after an invalid move and accepts the next one`() =
        runBlocking {
            val io = FakeGameIo("banana", "Paper")

            GameFlow(ChatLocks()).play(chatId = 1L, game = game, io = io)

            assertEquals(3, io.sent.size)
            assertTrue(io.sent[1].contains("banana"), io.sent[1])
            assertTrue(io.sent[1].contains("You have 2 tries left."), io.sent[1])
            assertTrue(io.sent[2].startsWith("You: Paper"), io.sent[2])
        }

    @Test
    fun `cancels after too many invalid attempts`() =
        runBlocking {
            val io = FakeGameIo("one", "two", "three")

            GameFlow(ChatLocks()).play(chatId = 1L, game = game, io = io)

            assertEquals(4, io.sent.size)
            assertEquals(tooManyAttemptsMessage, io.sent.last())
        }

    @Test
    fun `cancels when the player sends a command`() =
        runBlocking {
            val io = FakeGameIo("/cancel")

            GameFlow(ChatLocks()).play(chatId = 1L, game = game, io = io)

            assertEquals(2, io.sent.size)
            assertEquals(cancelledMessage, io.sent[1])
        }

    @Test
    fun `times out and releases the chat lock`() =
        runBlocking {
            val chatLocks = ChatLocks()
            val io = FakeGameIo()

            GameFlow(chatLocks, gameTimeout = 50.milliseconds)
                .play(chatId = 1L, game = game, io = io)

            assertEquals(timeoutMessage, io.sent.last())
            assertFalse(chatLocks.isBusy(1L))
            assertEquals(0, chatLocks.trackedChats())
        }

    @Test
    fun `rejects a second game in the same chat while the first is running`() =
        runBlocking {
            val chatLocks = ChatLocks()
            val holder = chatLocks.acquire(1L)
            assertTrue(holder.tryLock())

            try {
                val io = FakeGameIo("Rock")
                GameFlow(chatLocks).play(chatId = 1L, game = game, io = io)

                assertEquals(listOf(busyMessage), io.sent)
            } finally {
                holder.release()
            }

            assertFalse(chatLocks.isBusy(1L))
        }

    @Test
    fun `releases the chat lock after a normal game`() =
        runBlocking {
            val chatLocks = ChatLocks()

            GameFlow(chatLocks).play(chatId = 9L, game = game, io = FakeGameIo("Rock"))

            assertFalse(chatLocks.isBusy(9L))
        }
}

private class FakeGameIo(
    vararg inputs: String,
) : GameIo {
    private val pending = ArrayDeque(inputs.toList())
    val sent = mutableListOf<String>()

    override suspend fun send(
        text: String,
        keyboard: KeyboardMarkup?,
    ) {
        sent += text
    }

    override suspend fun awaitText(): String = pending.removeFirstOrNull() ?: awaitCancellation()
}

private class FixedClassicGame : Game<ClassicMoveType> {
    override val moves: List<ClassicMoveType> = ClassicMoveType.entries

    override fun play(playerMove: ClassicMoveType): RoundResult<ClassicMoveType> =
        RoundResult(
            playerMove = playerMove,
            botMove = ClassicMoveType.SCISSORS,
            outcome = GameOutcome.WIN,
            rulePhrase = "Rock crushes Scissors",
        )
}
