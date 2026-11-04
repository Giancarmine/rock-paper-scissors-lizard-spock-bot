package com.el_giancar.srv

import com.el_giancar.model.SheldonMoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheldonGameServiceTest {
    private val service = SheldonGameService()

    @Test
    fun `resolves every Sheldon combination`() {
        val wins = mapOf(
            SheldonMoveType.ROCK to setOf(SheldonMoveType.SCISSORS, SheldonMoveType.LIZARD),
            SheldonMoveType.PAPER to setOf(SheldonMoveType.ROCK, SheldonMoveType.SPOCK),
            SheldonMoveType.SCISSORS to setOf(SheldonMoveType.PAPER, SheldonMoveType.LIZARD),
            SheldonMoveType.LIZARD to setOf(SheldonMoveType.PAPER, SheldonMoveType.SPOCK),
            SheldonMoveType.SPOCK to setOf(SheldonMoveType.ROCK, SheldonMoveType.SCISSORS)
        )

        for (player in SheldonMoveType.values()) {
            for (computer in SheldonMoveType.values()) {
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
            "You win! Spock vaporizes Rock.",
            service.play(SheldonMoveType.SPOCK, SheldonMoveType.ROCK)
        )
        assertEquals(
            "You lose! Lizard poisons Spock.",
            service.play(SheldonMoveType.SPOCK, SheldonMoveType.LIZARD)
        )
    }

    @Test
    fun `random play returns a valid result`() {
        repeat(20) {
            val result = service.play(SheldonMoveType.SPOCK)
            assertTrue(
                result == "It's a tie!" ||
                    result.startsWith("You win!") ||
                    result.startsWith("You lose!"),
                "Unexpected result: $result"
            )
        }
    }
}
