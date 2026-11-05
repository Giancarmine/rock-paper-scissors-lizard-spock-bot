package com.el_giancar

import com.el_giancar.application.ChatLocks
import com.el_giancar.application.ClassicGameService
import com.el_giancar.application.SheldonGameService
import com.el_giancar.config.AppConfig
import com.el_giancar.infrastructure.http.healthRoutes
import com.el_giancar.infrastructure.telegram.runTelegramBot
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking

fun main() {
    val config = AppConfig.fromEnvironment()

    // Start the HTTP server first so /healthz is reachable while the bot runs.
    // The Telegram long-polling loop then owns the main thread; if it fails, the
    // process exits and the server is stopped.
    val server = embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        healthRoutes()
    }
    server.start(wait = false)

    try {
        runBlocking {
            runTelegramBot(
                token = config.botToken,
                classicGameService = ClassicGameService(),
                sheldonGameService = SheldonGameService(),
                chatLocks = ChatLocks(),
            )
        }
    } finally {
        server.stop(gracePeriodMillis = 0, timeoutMillis = 5_000)
    }
}
