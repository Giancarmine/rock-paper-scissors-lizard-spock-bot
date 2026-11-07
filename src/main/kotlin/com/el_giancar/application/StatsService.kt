package com.el_giancar.application

import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.GameRecord
import com.el_giancar.domain.LeaderboardEntry
import com.el_giancar.domain.PersonalStats
import com.el_giancar.domain.Player
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory

/**
 * Optional score tracking.
 *
 * A `null` store means the feature is disabled because `DATABASE_URL` is not
 * configured. Runtime database failures are reported as [StatsResult.Unavailable]
 * and are never allowed to break a game.
 */
class StatsService(
    private val store: StatsStore?,
) {
    val enabled: Boolean
        get() = store != null

    suspend fun recordGame(
        player: Player,
        chatId: Long,
        outcome: GameOutcome,
    ) {
        val activeStore = store ?: return
        try {
            activeStore.recordGame(
                GameRecord(
                    player = player,
                    chatId = chatId,
                    outcome = outcome,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn(
                "Could not record a game; score tracking is temporarily unavailable ({})",
                exception.javaClass.simpleName,
            )
        }
    }

    suspend fun personalStats(
        playerId: Long,
        chatId: Long,
    ): StatsResult<PersonalStats> = withStore { it.personalStats(playerId, chatId) }

    suspend fun globalLeaderboard(limit: Int = DEFAULT_LIMIT): StatsResult<List<LeaderboardEntry>> =
        withStore { it.globalLeaderboard(limit) }

    suspend fun chatLeaderboard(
        chatId: Long,
        limit: Int = DEFAULT_LIMIT,
    ): StatsResult<List<LeaderboardEntry>> = withStore { it.chatLeaderboard(chatId, limit) }

    fun close() {
        try {
            store?.close()
        } catch (exception: Exception) {
            logger.warn(
                "Could not close the score database cleanly ({})",
                exception.javaClass.simpleName,
            )
        }
    }

    private suspend fun <T> withStore(block: suspend (StatsStore) -> T): StatsResult<T> {
        val activeStore = store ?: return StatsResult.Disabled
        return try {
            StatsResult.Success(block(activeStore))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.warn(
                "Could not read scores; score tracking is temporarily unavailable ({})",
                exception.javaClass.simpleName,
            )
            StatsResult.Unavailable
        }
    }

    companion object {
        const val DEFAULT_LIMIT: Int = 10

        private val logger = LoggerFactory.getLogger(StatsService::class.java)
    }
}

/** The outcome of an optional statistics read. */
sealed interface StatsResult<out T> {
    /** Score tracking is disabled because no database is configured. */
    data object Disabled : StatsResult<Nothing>

    /** Score tracking is configured but the database could not be reached. */
    data object Unavailable : StatsResult<Nothing>

    data class Success<T>(
        val value: T,
    ) : StatsResult<T>
}
