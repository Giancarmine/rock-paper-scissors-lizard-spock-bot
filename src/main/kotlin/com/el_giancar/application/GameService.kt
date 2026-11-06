package com.el_giancar.application

import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.Move
import com.el_giancar.domain.RoundResult
import java.util.Random

/**
 * Shared round resolution for every game variant.
 *
 * The service only returns data ([RoundResult]); choosing user-facing wording is the
 * job of the presentation layer.
 */
open class GameService<M : Move>(
    private val rules: GameRules<M>,
    private val random: Random = Random(),
) : Game<M> {
    override val moves: List<M>
        get() = rules.moves

    override fun play(playerMove: M): RoundResult<M> = play(playerMove, randomMove())

    /**
     * Plays a deterministic round. Useful for tests and for callers that already
     * know the opponent's move.
     */
    fun play(
        playerMove: M,
        botMove: M,
    ): RoundResult<M> {
        val playerPhrase = rules.phrase(playerMove, botMove)
        val botPhrase = rules.phrase(botMove, playerMove)

        return when {
            playerMove == botMove ->
                RoundResult(
                    playerMove = playerMove,
                    botMove = botMove,
                    outcome = GameOutcome.TIE,
                    rulePhrase = null,
                )
            playerPhrase != null ->
                RoundResult(
                    playerMove = playerMove,
                    botMove = botMove,
                    outcome = GameOutcome.WIN,
                    rulePhrase = playerPhrase,
                )
            botPhrase != null ->
                RoundResult(
                    playerMove = playerMove,
                    botMove = botMove,
                    outcome = GameOutcome.LOSE,
                    rulePhrase = botPhrase,
                )
            else -> error("No rule for $playerMove vs $botMove")
        }
    }

    private fun randomMove(): M = rules.moves[random.nextInt(rules.moves.size)]
}
