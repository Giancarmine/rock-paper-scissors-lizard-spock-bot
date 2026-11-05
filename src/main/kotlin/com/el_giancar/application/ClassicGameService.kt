package com.el_giancar.application

import com.el_giancar.domain.ClassicMoveType
import java.util.Random

/** Rock, Paper, Scissors. */
class ClassicGameService(random: Random = Random()) :
    GameService<ClassicMoveType>(ClassicRules, random)

private object ClassicRules : GameRules<ClassicMoveType> {
    override val moves: List<ClassicMoveType> = ClassicMoveType.entries

    private val phrases = mapOf(
        (ClassicMoveType.ROCK to ClassicMoveType.SCISSORS) to "Rock crushes Scissors",
        (ClassicMoveType.PAPER to ClassicMoveType.ROCK) to "Paper covers Rock",
        (ClassicMoveType.SCISSORS to ClassicMoveType.PAPER) to "Scissors cuts Paper",
    )

    override fun phrase(winner: ClassicMoveType, loser: ClassicMoveType): String? =
        phrases[winner to loser]
}
