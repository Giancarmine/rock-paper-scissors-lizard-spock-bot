package com.el_giancar

import com.el_giancar.plugins.configureTelegram
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

fun main() {
    val botToken = System.getenv("BOT_TOKEN")
        ?.takeIf { it.isNotBlank() }
        ?: error(
            "BOT_TOKEN is not set. Create a bot with @BotFather and export the token " +
                "before starting the application."
        )
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    // Start the HTTP server first so /healthz is reachable while the bot runs.
    // The Telegram long-polling loop then owns the main thread; if it fails, the
    // process exits and the server is stopped.
    val server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureHttp()
    }
    server.start(wait = false)

    try {
        runBlocking { configureTelegram(botToken) }
    } finally {
        server.stop(gracePeriodMillis = 0, timeoutMillis = 5_000)
    }
}

fun Application.configureHttp() {
    routing {
        get("/healthz") {
            call.respondText("OK")
        }
    }
}
