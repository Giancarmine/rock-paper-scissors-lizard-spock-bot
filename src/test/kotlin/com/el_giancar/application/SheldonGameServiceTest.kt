package com.el_giancar.application

import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.SheldonMoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SheldonGameServiceTest {
    private val service = SheldonGameService()

    @Test
    fun `resolves every Sheldon combination`() {
        val wins =
            mapOf(
                SheldonMoveType.ROCK to setOf(SheldonMoveType.SCISSORS, SheldonMoveType.LIZARD),
                SheldonMoveType.PAPER to setOf(SheldonMoveType.ROCK, SheldonMoveType.SPOCK),
                SheldonMoveType.SCISSORS to setOf(SheldonMoveType.PAPER, SheldonMoveType.LIZARD),
                SheldonMoveType.LIZARD to setOf(SheldonMoveType.PAPER, SheldonMoveType.SPOCK),
                SheldonMoveType.SPOCK to setOf(SheldonMoveType.ROCK, SheldonMoveType.SCISSORS),
            )

        for (player in SheldonMoveType.entries) {
            for (bot in SheldonMoveType.entries) {
                val result = service.play(player, bot)
                assertEquals(player, result.playerMove)
                assertEquals(bot, result.botMove)
                when {
                    player == bot -> {
                        assertEquals(GameOutcome.TIE, result.outcome)
                        assertNull(result.rulePhrase)
                    }
                    bot in wins.getValue(player) -> {
                        assertEquals(GameOutcome.WIN, result.outcome)
                        assertNotNull(result.rulePhrase)
                    }
                    else -> {
                        assertEquals(GameOutcome.LOSE, result.outcome)
                        assertNotNull(result.rulePhrase)
                    }
                }
            }
        }
    }

    @Test
    fun `uses the canonical winning phrases`() {
        val win = service.play(SheldonMoveType.SPOCK, SheldonMoveType.ROCK)
        assertEquals(GameOutcome.WIN, win.outcome)
        assertEquals("Spock vaporizes Rock", win.rulePhrase)

        val loss = service.play(SheldonMoveType.SPOCK, SheldonMoveType.LIZARD)
        assertEquals(GameOutcome.LOSE, loss.outcome)
        assertEquals("Lizard poisons Spock", loss.rulePhrase)
    }

    @Test
    fun `random play returns a valid result`() {
        repeat(20) {
            val result = service.play(SheldonMoveType.SPOCK)
            assertEquals(SheldonMoveType.SPOCK, result.playerMove)
            assertTrue(result.botMove in SheldonMoveType.entries)
            if (result.outcome == GameOutcome.TIE) {
                assertNull(result.rulePhrase)
            } else {
                assertNotNull(result.rulePhrase)
            }
        }
    }
}
