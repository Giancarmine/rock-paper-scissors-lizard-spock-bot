package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.Move
import com.el_giancar.domain.RoundResult

internal val startMessage =
    """
    |👋 Welcome to Rock–Paper–Scissors–Lizard–Spock!
    |
    |I pick a random move, you pick yours, and I explain the result.
    |
    |Pick a game below, or send /help for the full command list.
    """.trimMargin()

internal val rulesMessage =
    """
    |🧠 Rock–Paper–Scissors–Lizard–Spock
    |
    |✂️ Scissors cuts Paper 📄
    |📄 Paper covers Rock 🪨
    |🪨 Rock crushes Lizard 🦎
    |🦎 Lizard poisons Spock 🖖
    |🖖 Spock smashes Scissors ✂️
    |✂️ Scissors decapitates Lizard 🦎
    |🦎 Lizard eats Paper 📄
    |📄 Paper disproves Spock 🖖
    |🖖 Spock vaporizes Rock 🪨
    |🪨 Rock crushes Scissors ✂️
    |
    |Classic mode uses only Rock, Paper and Scissors.
    """.trimMargin()

internal val helpMessage =
    """
    |🤖 Commands
    |
    |/start — show the welcome message
    |/classic_game — play Rock, Paper, Scissors
    |/sheldon_game — play Rock, Paper, Scissors, Lizard, Spock
    |/cancel — cancel the current game
    |/rules — show which move beats which
    |/commands — show the command keyboard
    |/about — show info and source code
    |/help — show this message
    |
    |During a game, send /cancel or any other command to end it.
    """.trimMargin()

internal val aboutMessage =
    """
    |🖖 I'm a small open-source Telegram bot for Rock–Paper–Scissors and
    |Rock–Paper–Scissors–Lizard–Spock.
    |
    |Built with Kotlin, Ktor and tgbotapi, and released under the MIT license.
    |Source: https://github.com/Giancarmine/rock-paper-scissors-lizard-spock-bot
    """.trimMargin()

internal val commandsMenuMessage = "🎮 Pick a game or a command:"

internal val busyMessage =
    "⏳ A game is already running in this chat. Play your move, or send /cancel to end it."

internal val cancelledMessage = "🚫 Game cancelled. Start a new one whenever you're ready."

internal val noActiveGameMessage =
    "🤔 No game is running right now. Start one with /classic_game or /sheldon_game."

internal val idleTextMessage =
    "🎮 I can't chat, but I can play! Start a game with /classic_game or " +
        "/sheldon_game, or send /commands to see everything I can do."

internal val timeoutMessage = "⌛ This game timed out. Start a new one whenever you're ready."

internal val tooManyAttemptsMessage =
    "❌ Too many invalid moves, so I cancelled the game. Start a new one whenever you're ready."

internal val unknownCommandMessage =
    "🤔 I don't know that command. Send /commands to see what I can do."

internal fun movePrompt(moves: List<Move>): String =
    """
    |🎮 Your move!
    |
    |Tap a button below or type one of: ${moves.joinToString(", ") { it.displayName }}.
    |Send /cancel (or any other command) to cancel the game.
    """.trimMargin()

internal fun unknownMoveMessage(
    moves: List<Move>,
    input: String,
    attemptsLeft: Int,
): String {
    val shown =
        input.trim().replace(Regex("\\s+"), " ").let {
            if (it.length > 32) it.take(32) + "…" else it
        }
    val tries = if (attemptsLeft == 1) "1 try" else "$attemptsLeft tries"
    return "🤔 \"$shown\" isn't a valid move. Choose one of: " +
        moves.joinToString(", ") { it.displayName } + ".\n" +
        "You have $tries left."
}

internal fun resultMessage(result: RoundResult<*>): String =
    buildString {
        append("You: ").append(result.playerMove.displayName).append('\n')
        append("Me: ").append(result.botMove.displayName).append("\n\n")
        when (result.outcome) {
            GameOutcome.WIN -> append("🎉 You win! ").append(requireNotNull(result.rulePhrase)).append('.')
            GameOutcome.LOSE -> append("😅 I win this round! ").append(requireNotNull(result.rulePhrase)).append('.')
            GameOutcome.TIE -> append("🤝 It's a tie!")
        }
    }
