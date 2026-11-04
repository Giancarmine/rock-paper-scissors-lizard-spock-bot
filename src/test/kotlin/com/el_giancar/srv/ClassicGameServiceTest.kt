package com.el_giancar.srv

import com.el_giancar.model.ClassicMoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassicGameServiceTest {
    private val service = ClassicGameService()

    @Test
    fun `resolves every classic combination`() {
        val wins = mapOf(
            ClassicMoveType.ROCK to setOf(ClassicMoveType.SCISSORS),
            ClassicMoveType.PAPER to setOf(ClassicMoveType.ROCK),
            ClassicMoveType.SCISSORS to setOf(ClassicMoveType.PAPER)
        )

        for (player in ClassicMoveType.values()) {
            for (computer in ClassicMoveType.values()) {
                val result = service.play(player, computer)
                when {
                    player == computer -> assertEquals("It's a tie!", result)
                    computer in wins.getValue(player) -> assertTrue(
                        result.startsWith("You win!"),
                        "Expected a win for $player vs $computer, got: $result"
                    )
                    else -> assertTrue(
                        result.startsWith("You lose!"),
                        "Expected a loss for $player vs $computer, got: $result"
                    )
                }
            }
        }
    }

    @Test
    fun `uses the canonical winning phrases`() {
        assertEquals(
            "You win! Rock crushes Scissors.",
            service.play(ClassicMoveType.ROCK, ClassicMoveType.SCISSORS)
        )
        assertEquals(
            "You lose! Paper covers Rock.",
            service.play(ClassicMoveType.ROCK, ClassicMoveType.PAPER)
        )
    }

    @Test
    fun `random play returns a valid result`() {
        repeat(20) {
            val result = service.play(ClassicMoveType.ROCK)
            assertTrue(
                result == "It's a tie!" ||
                    result.startsWith("You win!") ||
                    result.startsWith("You lose!"),
                "Unexpected result: $result"
            )
        }
    }
}
