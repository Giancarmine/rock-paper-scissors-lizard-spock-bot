package com.el_giancar.application

import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.GameOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

        for (player in ClassicMoveType.entries) {
            for (bot in ClassicMoveType.entries) {
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
        val win = service.play(ClassicMoveType.ROCK, ClassicMoveType.SCISSORS)
        assertEquals(GameOutcome.WIN, win.outcome)
        assertEquals("Rock crushes Scissors", win.rulePhrase)

        val loss = service.play(ClassicMoveType.ROCK, ClassicMoveType.PAPER)
        assertEquals(GameOutcome.LOSE, loss.outcome)
        assertEquals("Paper covers Rock", loss.rulePhrase)
    }

    @Test
    fun `random play returns a valid result`() {
        repeat(20) {
            val result = service.play(ClassicMoveType.ROCK)
            assertEquals(ClassicMoveType.ROCK, result.playerMove)
            assertTrue(result.botMove in ClassicMoveType.entries)
            if (result.outcome == GameOutcome.TIE) {
                assertNull(result.rulePhrase)
            } else {
                assertNotNull(result.rulePhrase)
            }
        }
    }
}
