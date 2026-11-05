package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.RoundResult
import com.el_giancar.domain.SheldonMoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagesTest {
    @Test
    fun `shows both moves and the winning phrase`() {
        val message = resultMessage(
            RoundResult(
                playerMove = ClassicMoveType.ROCK,
                botMove = ClassicMoveType.SCISSORS,
                outcome = GameOutcome.WIN,
                rulePhrase = "Rock crushes Scissors",
            )
        )

        assertEquals(
            """
            |You: Rock
            |Me: Scissors
            |
            |🎉 You win! Rock crushes Scissors.
            """.trimMargin(),
            message,
        )
    }

    @Test
    fun `formats a loss from the player's point of view`() {
        val message = resultMessage(
            RoundResult(
                playerMove = ClassicMoveType.ROCK,
                botMove = ClassicMoveType.PAPER,
                outcome = GameOutcome.LOSE,
                rulePhrase = "Paper covers Rock",
            )
        )

        assertTrue(message.contains("You: Rock"))
        assertTrue(message.contains("Me: Paper"))
        assertTrue(message.contains("😅 I win this round! Paper covers Rock."))
    }

    @Test
    fun `formats a tie without a winning phrase`() {
        val message = resultMessage(
            RoundResult(
                playerMove = ClassicMoveType.SCISSORS,
                botMove = ClassicMoveType.SCISSORS,
                outcome = GameOutcome.TIE,
                rulePhrase = null,
            )
        )

        assertEquals(
            """
            |You: Scissors
            |Me: Scissors
            |
            |🤝 It's a tie!
            """.trimMargin(),
            message,
        )
    }

    @Test
    fun `lists every legal move in the prompt and in the invalid-move hint`() {
        val moves = SheldonMoveType.entries
        val prompt = movePrompt(moves)
        val hint = unknownMoveMessage(moves, "banana")

        for (move in moves) {
            assertTrue(prompt.contains(move.displayName), "Prompt is missing ${move.displayName}: $prompt")
            assertTrue(hint.contains(move.displayName), "Hint is missing ${move.displayName}: $hint")
        }
    }
}
