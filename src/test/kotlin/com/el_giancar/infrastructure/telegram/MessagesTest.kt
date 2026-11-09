package com.el_giancar.infrastructure.telegram

import com.el_giancar.application.StatsResult
import com.el_giancar.domain.ClassicMoveType
import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.LeaderboardEntry
import com.el_giancar.domain.PersonalStats
import com.el_giancar.domain.PlayerStats
import com.el_giancar.domain.RoundResult
import com.el_giancar.domain.SheldonMoveType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessagesTest {
    @Test
    fun `shows both moves and the winning phrase`() {
        val message =
            resultMessage(
                RoundResult(
                    playerMove = ClassicMoveType.ROCK,
                    botMove = ClassicMoveType.SCISSORS,
                    outcome = GameOutcome.WIN,
                    rulePhrase = "Rock crushes Scissors",
                ),
            )

        assertEquals(
            """
            |You: Rock
            |Me: Scissors
            |
            |🎉 You win! Rock crushes Scissors.
            """.trimMargin(),
            message,
        )
    }

    @Test
    fun `formats a loss from the player's point of view`() {
        val message =
            resultMessage(
                RoundResult(
                    playerMove = ClassicMoveType.ROCK,
                    botMove = ClassicMoveType.PAPER,
                    outcome = GameOutcome.LOSE,
                    rulePhrase = "Paper covers Rock",
                ),
            )

        assertTrue(message.contains("You: Rock"))
        assertTrue(message.contains("Me: Paper"))
        assertTrue(message.contains("😅 I win this round! Paper covers Rock."))
    }

    @Test
    fun `formats a tie without a winning phrase`() {
        val message =
            resultMessage(
                RoundResult(
                    playerMove = ClassicMoveType.SCISSORS,
                    botMove = ClassicMoveType.SCISSORS,
                    outcome = GameOutcome.TIE,
                    rulePhrase = null,
                ),
            )

        assertEquals(
            """
            |You: Scissors
            |Me: Scissors
            |
            |🤝 It's a tie!
            """.trimMargin(),
            message,
        )
    }

    @Test
    fun `lists every legal move in the prompt and in the invalid-move hint`() {
        val moves = SheldonMoveType.entries
        val prompt = movePrompt(moves)
        val hint = unknownMoveMessage(moves, "banana", attemptsLeft = 2)

        for (move in moves) {
            assertTrue(prompt.contains(move.displayName), "Prompt is missing ${move.displayName}: $prompt")
            assertTrue(hint.contains(move.displayName), "Hint is missing ${move.displayName}: $hint")
        }
    }

    @Test
    fun `truncates a long invalid move and reports the remaining tries`() {
        val hint = unknownMoveMessage(ClassicMoveType.entries, "x".repeat(50), attemptsLeft = 2)

        assertTrue(hint.contains("\"${"x".repeat(32)}…\""), hint)
        assertTrue(hint.contains("You have 2 tries left."), hint)
        assertFalse(hint.contains("x".repeat(33)), hint)
    }

    @Test
    fun `uses singular wording for the last try`() {
        val hint = unknownMoveMessage(ClassicMoveType.entries, "banana", attemptsLeft = 1)

        assertTrue(hint.contains("You have 1 try left."), hint)
    }

    @Test
    fun `help lists every command`() {
        val commands =
            listOf(
                "/start",
                "/classic_game",
                "/sheldon_game",
                "/cancel",
                "/rules",
                "/stats",
                "/leaderboard",
                "/chat_leaderboard",
                "/commands",
                "/about",
                "/help",
            )

        for (command in commands) {
            assertTrue(helpMessage.contains(command), "Help is missing $command: $helpMessage")
        }
    }

    @Test
    fun `about mentions the license`() {
        assertTrue(aboutMessage.contains("MIT"), aboutMessage)
    }

    @Test
    fun `advertises the cancel command in help and in the move prompt`() {
        val prompt = movePrompt(ClassicMoveType.entries)

        assertTrue(helpMessage.contains("/cancel"), "Help is missing /cancel: $helpMessage")
        assertTrue(prompt.contains("/cancel"), "Prompt is missing /cancel: $prompt")
    }

    @Test
    fun `explains that there is nothing to cancel when no game is running`() {
        assertTrue(noActiveGameMessage.contains("/classic_game"))
        assertTrue(noActiveGameMessage.contains("/sheldon_game"))
    }

    @Test
    fun `points idle text at the game commands and the command list`() {
        assertTrue(idleTextMessage.contains("/classic_game"), idleTextMessage)
        assertTrue(idleTextMessage.contains("/sheldon_game"), idleTextMessage)
        assertTrue(idleTextMessage.contains("/commands"), idleTextMessage)
    }

    @Test
    fun `uses a share-specific greeting for the share deep link`() {
        assertEquals(shareStartMessage, startMessageFor("share"))
        assertEquals(shareStartMessage, startMessageFor("SHARE"))
        assertEquals(startMessage, startMessageFor(null))
        assertEquals(startMessage, startMessageFor("unknown"))
    }

    @Test
    fun `group welcome points at the group commands`() {
        assertTrue(groupWelcomeMessage.contains("/classic_game"), groupWelcomeMessage)
        assertTrue(groupWelcomeMessage.contains("/sheldon_game"), groupWelcomeMessage)
        assertTrue(groupWelcomeMessage.contains("/chat_leaderboard"), groupWelcomeMessage)
    }

    @Test
    fun `formats personal stats for this chat and overall`() {
        val stats =
            PersonalStats(
                overall =
                    PlayerStats(
                        games = 3,
                        wins = 1,
                        losses = 1,
                        ties = 1,
                        points = 1,
                        lastPlayed = Instant.parse("2026-11-08T11:30:00Z"),
                    ),
                chat =
                    PlayerStats(
                        games = 2,
                        wins = 1,
                        losses = 1,
                        ties = 0,
                        points = 1,
                        lastPlayed = Instant.parse("2026-11-08T11:30:00Z"),
                    ),
            )

        assertEquals(
            """
            |📊 Your stats
            |
            |This chat
            |Games: 2 · Wins: 1 · Losses: 1 · Ties: 0
            |Points: 1
            |
            |All chats
            |Games: 3 · Wins: 1 · Losses: 1 · Ties: 1
            |Points: 1
            |
            |Last played: 2026-11-08 11:30 UTC
            """.trimMargin(),
            statsMessage(StatsResult.Success(stats)),
        )
    }

    @Test
    fun `explains when the player has no finished games`() {
        val stats = PersonalStats(PlayerStats.empty(), PlayerStats.empty())

        assertEquals(noPersonalStatsMessage, statsMessage(StatsResult.Success(stats)))
        assertTrue(noPersonalStatsMessage.contains("/classic_game"))
    }

    @Test
    fun `formats global leaderboard ranks and pluralisation`() {
        val entries =
            listOf(
                LeaderboardEntry(1, 1L, "Alice", points = 2, games = 2, wins = 2),
                LeaderboardEntry(1, 2L, "Bob", points = 2, games = 2, wins = 2),
                LeaderboardEntry(3, 3L, "Carol", points = 1, games = 1, wins = 1),
            )

        assertEquals(
            """
            |🏆 Global leaderboard
            |
            |1. Alice — 2 points · 2 games
            |
            |1. Bob — 2 points · 2 games
            |
            |3. Carol — 1 point · 1 game
            """.trimMargin(),
            globalLeaderboardMessage(StatsResult.Success(entries)),
        )
    }

    @Test
    fun `formats the chat leaderboard title`() {
        val entries = listOf(LeaderboardEntry(1, 1L, "Alice", points = 1, games = 1, wins = 1))

        assertTrue(
            chatLeaderboardMessage(StatsResult.Success(entries)).startsWith("🏆 This chat's leaderboard"),
        )
    }

    @Test
    fun `explains an empty leaderboard`() {
        assertEquals(emptyLeaderboardMessage, globalLeaderboardMessage(StatsResult.Success(emptyList())))
        assertEquals(emptyLeaderboardMessage, chatLeaderboardMessage(StatsResult.Success(emptyList())))
    }

    @Test
    fun `explains disabled and unavailable score tracking`() {
        assertEquals(statsDisabledMessage, statsMessage(StatsResult.Disabled))
        assertEquals(statsUnavailableMessage, statsMessage(StatsResult.Unavailable))
        assertEquals(statsDisabledMessage, globalLeaderboardMessage(StatsResult.Disabled))
        assertEquals(statsUnavailableMessage, chatLeaderboardMessage(StatsResult.Unavailable))
        assertTrue(statsDisabledMessage.contains("Games still work"))
        assertTrue(statsUnavailableMessage.contains("Games still work"))
    }
}
