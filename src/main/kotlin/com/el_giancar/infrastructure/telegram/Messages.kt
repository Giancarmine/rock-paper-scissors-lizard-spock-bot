package com.el_giancar.infrastructure.telegram

import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.Move
import com.el_giancar.domain.RoundResult

internal val startMessage = """
    |👋 Welcome to Rock–Paper–Scissors–Lizard–Spock!
    |
    |Pick a game below, or use /help to see everything I can do.
""".trimMargin()

internal val rulesMessage = """
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

internal val helpMessage = """
    |🤖 Commands
    |
    |/classic_game — Rock, Paper, Scissors
    |/sheldon_game — Rock, Paper, Scissors, Lizard, Spock
    |/rules — show the winning moves
    |/commands — show the command keyboard
    |/about — about this bot
    |/help — show this message
    |
    |During a game, send any command to cancel it.
""".trimMargin()

internal val aboutMessage = """
    |🖖 I'm a small open-source Telegram bot for Rock–Paper–Scissors and
    |Rock–Paper–Scissors–Lizard–Spock.
    |
    |Built with Kotlin, Ktor and tgbotapi.
    |Source: https://github.com/Giancarmine/rock-paper-scissors-lizard-spock-bot
""".trimMargin()

internal val commandsMenuMessage = "🎮 Choose a game or a command:"

internal val busyMessage =
    "⏳ A game is already in progress in this chat. Finish it, or send any command to cancel it."

internal val cancelledMessage = "🚫 Game cancelled. Start a new one whenever you're ready."

internal val timeoutMessage = "⌛ This game timed out. Start a new one whenever you're ready."

internal val tooManyAttemptsMessage =
    "❌ Too many invalid moves — game cancelled. Start a new one whenever you're ready."

internal val unknownCommandMessage =
    "🤔 I don't know that command. Use /commands to see what I can do."

internal fun movePrompt(moves: List<Move>): String = """
    |🎮 Your move!
    |
    |Tap a button below or type one of: ${moves.joinToString(", ") { it.displayName }}.
    |Any command cancels the game.
""".trimMargin()

internal fun unknownMoveMessage(moves: List<Move>, input: String): String =
    "🤔 \"${input.take(32)}\" isn't a valid move. Choose one of: " +
        moves.joinToString(", ") { it.displayName } + "."

internal fun resultMessage(result: RoundResult<*>): String = buildString {
    append("You: ").append(result.playerMove.displayName).append('\n')
    append("Me: ").append(result.botMove.displayName).append("\n\n")
    when (result.outcome) {
        GameOutcome.WIN -> append("🎉 You win! ").append(requireNotNull(result.rulePhrase)).append('.')
        GameOutcome.LOSE -> append("😅 I win this round! ").append(requireNotNull(result.rulePhrase)).append('.')
        GameOutcome.TIE -> append("🤝 It's a tie!")
    }
}
