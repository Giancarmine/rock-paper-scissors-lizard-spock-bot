package com.el_giancar.infrastructure.stats

import com.el_giancar.application.StatsStore
import com.el_giancar.domain.GameOutcome
import com.el_giancar.domain.GameRecord
import com.el_giancar.domain.LeaderboardEntry
import com.el_giancar.domain.PersonalStats
import com.el_giancar.domain.PlayerStats
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.time.Instant

/**
 * PostgreSQL implementation backed by a small HikariCP pool.
 *
 * The schema is created idempotently by [initialize] before the bot starts polling.
 * The connection URL may be a plain PostgreSQL URL (`postgresql://...`) as handed
 * out by Neon or Supabase, or a JDBC URL; credentials embedded in the URL are moved
 * into Hikari's username/password fields so they never appear in the JDBC URL.
 */
class PostgresStatsStore private constructor(
    private val dataSource: HikariDataSource,
) : StatsStore {
    // Games are serialized per chat, but one player can finish games in different
    // chats at the same time. Serializing writes avoids an update-then-insert race
    // in the two upserts without relying on dialect-specific ON CONFLICT syntax.
    private val writeMutex = Mutex()

    fun initialize() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                SCHEMA.forEach { statement.execute(it) }
            }
        }
    }

    override suspend fun recordGame(record: GameRecord) =
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                dataSource.connection.use { connection ->
                    connection.autoCommit = false
                    try {
                        upsertPlayer(connection, record)
                        incrementChatStats(connection, record)
                        connection.commit()
                    } catch (exception: Exception) {
                        connection.rollback()
                        throw exception
                    } finally {
                        connection.autoCommit = true
                    }
                }
            }
        }

    override suspend fun personalStats(
        playerId: Long,
        chatId: Long,
    ): PersonalStats =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                PersonalStats(
                    overall = readStats(connection, playerId, chatId = null),
                    chat = readStats(connection, playerId, chatId = chatId),
                )
            }
        }

    override suspend fun globalLeaderboard(limit: Int): List<LeaderboardEntry> =
        withContext(Dispatchers.IO) {
            require(limit > 0) { "limit must be positive" }
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(GLOBAL_LEADERBOARD_SQL)
                    .use { statement ->
                        statement.setInt(1, limit)
                        statement.executeQuery().use { rows ->
                            buildList {
                                while (rows.next()) {
                                    add(
                                        LeaderboardEntry(
                                            rank = 0,
                                            playerId = rows.getLong(1),
                                            displayName = rows.getString(2),
                                            points = rows.getInt(3),
                                            games = rows.getInt(4),
                                            wins = rows.getInt(5),
                                        ),
                                    )
                                }
                            }.withCompetitionRanks()
                        }
                    }
            }
        }

    override suspend fun chatLeaderboard(
        chatId: Long,
        limit: Int,
    ): List<LeaderboardEntry> =
        withContext(Dispatchers.IO) {
            require(limit > 0) { "limit must be positive" }
            dataSource.connection.use { connection ->
                connection
                    .prepareStatement(CHAT_LEADERBOARD_SQL)
                    .use { statement ->
                        statement.setLong(1, chatId)
                        statement.setInt(2, limit)
                        statement.executeQuery().use { rows ->
                            buildList {
                                while (rows.next()) {
                                    add(
                                        LeaderboardEntry(
                                            rank = 0,
                                            playerId = rows.getLong(1),
                                            displayName = rows.getString(2),
                                            points = rows.getInt(3),
                                            games = rows.getInt(4),
                                            wins = rows.getInt(5),
                                        ),
                                    )
                                }
                            }.withCompetitionRanks()
                        }
                    }
            }
        }

    override fun close() {
        dataSource.close()
    }

    private fun upsertPlayer(
        connection: Connection,
        record: GameRecord,
    ) {
        val playedAt = record.playedAt.toEpochMilli()
        val updated =
            connection
                .prepareStatement(
                    "UPDATE players SET display_name = ?, username = ?, last_played = ? WHERE user_id = ?",
                ).use { statement ->
                    statement.setString(1, record.player.displayName)
                    statement.setString(2, record.player.username)
                    statement.setLong(3, playedAt)
                    statement.setLong(4, record.player.id)
                    statement.executeUpdate()
                }

        if (updated == 0) {
            connection
                .prepareStatement(
                    "INSERT INTO players (user_id, display_name, username, first_seen, last_played) " +
                        "VALUES (?, ?, ?, ?, ?)",
                ).use { statement ->
                    statement.setLong(1, record.player.id)
                    statement.setString(2, record.player.displayName)
                    statement.setString(3, record.player.username)
                    statement.setLong(4, playedAt)
                    statement.setLong(5, playedAt)
                    statement.executeUpdate()
                }
        }
    }

    private fun incrementChatStats(
        connection: Connection,
        record: GameRecord,
    ) {
        val playedAt = record.playedAt.toEpochMilli()
        val won = if (record.outcome == GameOutcome.WIN) 1 else 0
        val lost = if (record.outcome == GameOutcome.LOSE) 1 else 0
        val tied = if (record.outcome == GameOutcome.TIE) 1 else 0

        val updated =
            connection
                .prepareStatement(
                    "UPDATE player_chat_stats SET games = games + 1, wins = wins + ?, " +
                        "losses = losses + ?, ties = ties + ?, points = points + ?, last_played = ? " +
                        "WHERE user_id = ? AND chat_id = ?",
                ).use { statement ->
                    statement.setInt(1, won)
                    statement.setInt(2, lost)
                    statement.setInt(3, tied)
                    statement.setInt(4, won)
                    statement.setLong(5, playedAt)
                    statement.setLong(6, record.player.id)
                    statement.setLong(7, record.chatId)
                    statement.executeUpdate()
                }

        if (updated == 0) {
            connection
                .prepareStatement(
                    "INSERT INTO player_chat_stats " +
                        "(user_id, chat_id, games, wins, losses, ties, points, last_played) " +
                        "VALUES (?, ?, 1, ?, ?, ?, ?, ?)",
                ).use { statement ->
                    statement.setLong(1, record.player.id)
                    statement.setLong(2, record.chatId)
                    statement.setInt(3, won)
                    statement.setInt(4, lost)
                    statement.setInt(5, tied)
                    statement.setInt(6, won)
                    statement.setLong(7, playedAt)
                    statement.executeUpdate()
                }
        }
    }

    private fun readStats(
        connection: Connection,
        playerId: Long,
        chatId: Long?,
    ): PlayerStats {
        val sql =
            if (chatId == null) {
                "SELECT COALESCE(SUM(games), 0), COALESCE(SUM(wins), 0), " +
                    "COALESCE(SUM(losses), 0), COALESCE(SUM(ties), 0), " +
                    "COALESCE(SUM(points), 0), MAX(last_played) " +
                    "FROM player_chat_stats WHERE user_id = ?"
            } else {
                "SELECT games, wins, losses, ties, points, last_played " +
                    "FROM player_chat_stats WHERE user_id = ? AND chat_id = ?"
            }

        return connection.prepareStatement(sql).use { statement ->
            statement.setLong(1, playerId)
            if (chatId != null) {
                statement.setLong(2, chatId)
            }
            statement.executeQuery().use { rows ->
                if (rows.next()) {
                    val lastPlayed = rows.getLong(6).takeUnless { rows.wasNull() }
                    PlayerStats(
                        games = rows.getInt(1),
                        wins = rows.getInt(2),
                        losses = rows.getInt(3),
                        ties = rows.getInt(4),
                        points = rows.getInt(5),
                        lastPlayed = lastPlayed?.let(Instant::ofEpochMilli),
                    )
                } else {
                    PlayerStats.empty()
                }
            }
        }
    }

    companion object {
        fun create(
            rawUrl: String,
            user: String? = null,
            password: String? = null,
        ): PostgresStatsStore {
            val settings = jdbcSettings(rawUrl, user, password)
            val config =
                HikariConfig().apply {
                    jdbcUrl = settings.url
                    username = settings.user
                    this.password = settings.password
                    maximumPoolSize = 4
                    minimumIdle = 1
                    connectionTimeout = 5_000
                    validationTimeout = 3_000
                    // Do not abort startup when the database is briefly unreachable.
                    initializationFailTimeout = 0
                    poolName = "stats-pool"
                }
            return PostgresStatsStore(HikariDataSource(config))
        }

        private val SCHEMA =
            listOf(
                """
                CREATE TABLE IF NOT EXISTS players (
                    user_id BIGINT PRIMARY KEY,
                    display_name VARCHAR(255) NOT NULL,
                    username VARCHAR(64),
                    first_seen BIGINT NOT NULL,
                    last_played BIGINT NOT NULL
                )
                """.trimIndent(),
                """
                CREATE TABLE IF NOT EXISTS player_chat_stats (
                    user_id BIGINT NOT NULL,
                    chat_id BIGINT NOT NULL,
                    games INTEGER NOT NULL DEFAULT 0,
                    wins INTEGER NOT NULL DEFAULT 0,
                    losses INTEGER NOT NULL DEFAULT 0,
                    ties INTEGER NOT NULL DEFAULT 0,
                    points INTEGER NOT NULL DEFAULT 0,
                    last_played BIGINT NOT NULL,
                    PRIMARY KEY (user_id, chat_id),
                    CONSTRAINT fk_player_chat_stats_player
                        FOREIGN KEY (user_id) REFERENCES players (user_id) ON DELETE CASCADE
                )
                """.trimIndent(),
                "CREATE INDEX IF NOT EXISTS idx_player_chat_stats_chat_points " +
                    "ON player_chat_stats (chat_id, points DESC)",
            )

        private const val GLOBAL_LEADERBOARD_SQL =
            "SELECT s.user_id, p.display_name, SUM(s.points), SUM(s.games), SUM(s.wins) " +
                "FROM player_chat_stats s JOIN players p ON p.user_id = s.user_id " +
                "GROUP BY s.user_id, p.display_name " +
                "ORDER BY SUM(s.points) DESC, SUM(s.wins) DESC, SUM(s.games) DESC, " +
                "MAX(s.last_played) ASC, s.user_id ASC LIMIT ?"

        private const val CHAT_LEADERBOARD_SQL =
            "SELECT s.user_id, p.display_name, s.points, s.games, s.wins " +
                "FROM player_chat_stats s JOIN players p ON p.user_id = s.user_id " +
                "WHERE s.chat_id = ? " +
                "ORDER BY s.points DESC, s.wins DESC, s.games DESC, s.last_played ASC, s.user_id ASC LIMIT ?"

        private fun List<LeaderboardEntry>.withCompetitionRanks(): List<LeaderboardEntry> {
            var rank = 0
            var previousPoints: Int? = null
            return mapIndexed { index, entry ->
                if (entry.points != previousPoints) {
                    rank = index + 1
                    previousPoints = entry.points
                }
                entry.copy(rank = rank)
            }
        }
    }
}

/** Connection settings after extracting credentials from a PostgreSQL URL. */
internal data class JdbcSettings(
    val url: String,
    val user: String?,
    val password: String?,
)

/**
 * Converts the URL formats handed out by hosted PostgreSQL providers into a JDBC
 * URL, moving `user:password@` out of the URL so it cannot be logged with it.
 */
internal fun jdbcSettings(
    rawUrl: String,
    user: String? = null,
    password: String? = null,
): JdbcSettings {
    val trimmed = rawUrl.trim()
    val withoutJdbcPrefix = trimmed.removePrefix("jdbc:")
    val normalized =
        if (withoutJdbcPrefix.startsWith("postgres://")) {
            "postgresql://" + withoutJdbcPrefix.removePrefix("postgres://")
        } else {
            withoutJdbcPrefix
        }

    if (!normalized.startsWith("postgresql://")) {
        return JdbcSettings(trimmed, user, password)
    }

    val uri = runCatching { URI(normalized) }.getOrNull()
    val host = uri?.host
    if (uri == null || host == null) {
        return JdbcSettings(trimmed, user, password)
    }

    val port = if (uri.port > 0) ":${uri.port}" else ""
    val path = uri.path?.takeIf { it.isNotEmpty() } ?: ""
    val queryPairs = uri.rawQuery?.split('&')?.filter { it.isNotEmpty() } ?: emptyList()
    val remainingQuery =
        queryPairs.filterNot { pair ->
            val key = pair.substringBefore('=')
            key.equals("user", ignoreCase = true) ||
                key.equals("username", ignoreCase = true) ||
                key.equals("password", ignoreCase = true)
        }
    val query = remainingQuery.takeIf { it.isNotEmpty() }?.joinToString(prefix = "?", separator = "&") ?: ""
    val jdbcUrl = "jdbc:postgresql://$host$port$path$query"

    val userInfo = uri.userInfo?.split(':', limit = 2)
    val urlUser = userInfo?.getOrNull(0)?.let(::decode)
    val urlPassword = userInfo?.getOrNull(1)?.let(::decode)

    val queryUser = queryParameter(queryPairs, "user", "username")
    val queryPassword = queryParameter(queryPairs, "password")

    return JdbcSettings(
        url = jdbcUrl,
        user = user ?: urlUser ?: queryUser,
        password = password ?: urlPassword ?: queryPassword,
    )
}

private fun queryParameter(
    pairs: List<String>,
    vararg names: String,
): String? =
    pairs
        .firstOrNull { pair -> names.any { name -> pair.substringBefore('=').equals(name, ignoreCase = true) } }
        ?.substringAfter('=', missingDelimiterValue = "")
        ?.takeIf { it.isNotEmpty() }
        ?.let(::decode)

private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)
