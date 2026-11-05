package com.el_giancar.domain

/**
 * A resolved round.
 *
 * [rulePhrase] describes the winning relation, for example `"Rock crushes Scissors"`.
 * It is `null` for a tie. Keeping the round data typed lets the presentation layer
 * choose wording and formatting without putting user-facing text in the game rules.
 */
data class RoundResult<out M : Move>(
    val playerMove: M,
    val botMove: M,
    val outcome: GameOutcome,
    val rulePhrase: String?,
) {
    init {
        require((outcome == GameOutcome.TIE) == (rulePhrase == null)) {
            "A tie has no winning phrase and a non-tie must have one"
        }
    }
}
