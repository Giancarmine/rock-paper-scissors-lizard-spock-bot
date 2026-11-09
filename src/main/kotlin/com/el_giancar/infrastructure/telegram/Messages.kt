package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.StatsResult
import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.LeaderboardEntry
import com.el_giancar.domain.Move
import com.el_giancar.domain.PersonalStats
import com.el_giancar.domain.PlayerStats
import com.el_giancar.domain.RoundResult
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal val startMessage =
    """
    |👋 Welcome to Rock–Paper–Scissors–Lizard–Spock!
    |
    |I pick a random move, you pick yours, and I explain the result.
    |
    |Pick a game below, or send /help for the full command list.
    |Track your score with /stats and see who leads with /leaderboard.
    """.trimMargin()

internal val shareStartMessage =
    """
    |👋 Welcome — someone shared this bot with you!
    |
    |I play Rock–Paper–Scissors and Rock–Paper–Scissors–Lizard–Spock against you.
    |
    |Pick a game below, or send /help for the full command list.
    |Track your score with /stats and see who leads with /leaderboard.
    """.trimMargin()

/**
 * The greeting for a `/start` deep link. Unknown payloads fall back to the normal
 * welcome so a crafted link cannot produce an empty or misleading reply.
 */
internal fun startMessageFor(payload: String?): String =
    when (payload?.lowercase()) {
        "share" -> shareStartMessage
        else -> startMessage
    }

internal val groupWelcomeMessage =
    """
    |👋 Thanks for adding me! I play Rock–Paper–Scissors and Rock–Paper–Scissors–Lizard–Spock right here in the chat.
    |
    |Start a round with /classic_game or /sheldon_game. Each game belongs to the person who starts it, so everyone can play.
    |Keep score with /stats and see who leads with /chat_leaderboard.
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
    |/stats — show your score in this chat and overall
    |/leaderboard — show the global top 10
    |/chat_leaderboard — show this chat's top 10
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

internal val statsDisabledMessage =
    "📊 Score tracking is disabled because the bot has no database configured.\n" +
        "Games still work; leaderboards are unavailable."

internal val statsUnavailableMessage =
    "⚠️ Score tracking is temporarily unavailable.\n" +
        "Games still work; please try again in a moment."

internal val noPersonalStatsMessage =
    "📊 You haven't finished a game yet.\n" +
        "Play one with /classic_game or /sheldon_game to start tracking your score."

internal val emptyLeaderboardMessage =
    "🏆 No games have been recorded yet.\n" +
        "Play one with /classic_game or /sheldon_game to get on the board."

internal fun statsMessage(result: StatsResult<PersonalStats>): String =
    when (result) {
        StatsResult.Disabled -> statsDisabledMessage
        StatsResult.Unavailable -> statsUnavailableMessage
        is StatsResult.Success -> personalStatsMessage(result.value)
    }

internal fun personalStatsMessage(stats: PersonalStats): String {
    if (stats.overall.games == 0) {
        return noPersonalStatsMessage
    }

    return buildString {
        append("📊 Your stats\n\n")
        append("This chat\n")
        append(statsSummary(stats.chat))
        append("\n\nAll chats\n")
        append(statsSummary(stats.overall))
        stats.overall.lastPlayed?.let {
            append("\n\nLast played: ").append(utcFormatter.format(it))
        }
    }
}

internal fun globalLeaderboardMessage(result: StatsResult<List<LeaderboardEntry>>): String =
    leaderboardMessage("🏆 Global leaderboard", result)

internal fun chatLeaderboardMessage(result: StatsResult<List<LeaderboardEntry>>): String =
    leaderboardMessage("🏆 This chat's leaderboard", result)

private fun leaderboardMessage(
    title: String,
    result: StatsResult<List<LeaderboardEntry>>,
): String =
    when (result) {
        StatsResult.Disabled -> statsDisabledMessage
        StatsResult.Unavailable -> statsUnavailableMessage
        is StatsResult.Success ->
            if (result.value.isEmpty()) {
                emptyLeaderboardMessage
            } else {
                buildString {
                    append(title)
                    result.value.forEach { entry ->
                        append("\n\n")
                        append(entry.rank).append(". ").append(entry.displayName).append(" — ")
                        append(entry.points).append(if (entry.points == 1) " point" else " points")
                        append(" · ").append(entry.games).append(if (entry.games == 1) " game" else " games")
                    }
                }
            }
    }

private fun statsSummary(stats: PlayerStats): String =
    buildString {
        append("Games: ").append(stats.games)
        append(" · Wins: ").append(stats.wins)
        append(" · Losses: ").append(stats.losses)
        append(" · Ties: ").append(stats.ties)
        append("\nPoints: ").append(stats.points)
    }

private val utcFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC)
