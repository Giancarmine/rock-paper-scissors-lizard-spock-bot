package com.el_giancar

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.ClassicGameService
import com.el_giancar.application.ReadinessState
import com.el_giancar.application.SheldonGameService
import com.el_giancar.application.StatsService
import com.el_giancar.config.AppConfig
import com.el_giancar.infrastructure.http.healthRoutes
import com.el_giancar.infrastructure.http.openApiRoutes
import com.el_giancar.infrastructure.logging.installJulToSlf4jBridge
import com.el_giancar.infrastructure.stats.PostgresStatsStore
import com.el_giancar.infrastructure.telegram.runTelegramBot
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.el_giancar.Application")

fun main() {
    // Must run before the Telegram client logs anything: tgbotapi uses
    // java.util.logging, which would otherwise bypass Logback's token redaction.
    installJulToSlf4jBridge()

    val config = AppConfig.fromEnvironment()
    val readiness = ReadinessState()
    val statsService = createStatsService(config)

    // Start the HTTP server first so /healthz is reachable while the bot runs.
    // The Telegram long-polling loop then owns the main thread; if it fails, the
    // process exits and the server is stopped.
    val server =
        embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
            healthRoutes(readiness)
            openApiRoutes()
        }
    server.start(wait = false)

    try {
        runBlocking {
            runTelegramBot(
                token = config.botToken,
                classicGame = ClassicGameService(),
                sheldonGame = SheldonGameService(),
                chatLocks = ChatLocks(),
                readiness = readiness,
                statsService = statsService,
            )
        }
    } finally {
        statsService.close()
        server.stop(gracePeriodMillis = 0, timeoutMillis = 5_000)
    }
}

/**
 * Builds the optional score service.
 *
 * Without `DATABASE_URL` the bot runs with statistics disabled. If the database
 * cannot be initialized, the bot logs only the exception type (never the URL or
 * credentials) and keeps playing without scores.
 */
private fun createStatsService(config: AppConfig): StatsService {
    val databaseUrl = config.databaseUrl ?: return StatsService(store = null)

    var store: PostgresStatsStore? = null
    return try {
        store =
            PostgresStatsStore.create(
                rawUrl = databaseUrl,
                user = config.databaseUser,
                password = config.databasePassword,
            )
        store.initialize()
        logger.info("Score tracking is enabled.")
        StatsService(store)
    } catch (exception: Exception) {
        runCatching { store?.close() }
        logger.warn(
            "Score tracking is disabled: the database could not be initialized ({}). " +
                "Check DATABASE_URL and database availability.",
            exception.javaClass.simpleName,
        )
        StatsService(store = null)
    }
}
