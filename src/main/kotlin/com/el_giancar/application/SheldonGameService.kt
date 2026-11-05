package com.el_giancar.application

import com.el_giancar.domain.SheldonMoveType
import java.util.Random

/** Rock, Paper, Scissors, Lizard, Spock. */
class SheldonGameService(random: Random = Random()) :
    GameService<SheldonMoveType>(SheldonRules, random)

private object SheldonRules : GameRules<SheldonMoveType> {
    override val moves: List<SheldonMoveType> = SheldonMoveType.entries

    private val phrases = mapOf(
        (SheldonMoveType.ROCK to SheldonMoveType.SCISSORS) to "Rock crushes Scissors",
        (SheldonMoveType.ROCK to SheldonMoveType.LIZARD) to "Rock crushes Lizard",
        (SheldonMoveType.PAPER to SheldonMoveType.ROCK) to "Paper covers Rock",
        (SheldonMoveType.PAPER to SheldonMoveType.SPOCK) to "Paper disproves Spock",
        (SheldonMoveType.SCISSORS to SheldonMoveType.PAPER) to "Scissors cuts Paper",
        (SheldonMoveType.SCISSORS to SheldonMoveType.LIZARD) to "Scissors decapitates Lizard",
        (SheldonMoveType.LIZARD to SheldonMoveType.PAPER) to "Lizard eats Paper",
        (SheldonMoveType.LIZARD to SheldonMoveType.SPOCK) to "Lizard poisons Spock",
        (SheldonMoveType.SPOCK to SheldonMoveType.ROCK) to "Spock vaporizes Rock",
        (SheldonMoveType.SPOCK to SheldonMoveType.SCISSORS) to "Spock smashes Scissors",
    )

    override fun phrase(winner: SheldonMoveType, loser: SheldonMoveType): String? =
        phrases[winner to loser]
}
