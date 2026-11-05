package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.SheldonMoveType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoveParserTest {
    @Test
    fun `accepts display names regardless of case and surrounding spaces`() {
        assertEquals(ClassicMoveType.ROCK, parseMove(ClassicMoveType.entries, "Rock"))
        assertEquals(ClassicMoveType.SCISSORS, parseMove(ClassicMoveType.entries, "SCISSORS"))
        assertEquals(ClassicMoveType.PAPER, parseMove(ClassicMoveType.entries, " paper "))
        assertEquals(SheldonMoveType.SPOCK, parseMove(SheldonMoveType.entries, "spock"))
        assertEquals(SheldonMoveType.LIZARD, parseMove(SheldonMoveType.entries, "LIZARD"))
    }

    @Test
    fun `rejects moves that are not part of the selected game`() {
        assertNull(parseMove(ClassicMoveType.entries, "Lizard"))
        assertNull(parseMove(ClassicMoveType.entries, "Spock"))
    }

    @Test
    fun `rejects unknown input`() {
        assertNull(parseMove(ClassicMoveType.entries, "banana"))
        assertNull(parseMove(ClassicMoveType.entries, ""))
        assertNull(parseMove(ClassicMoveType.entries, "/classic_game"))
    }
}
