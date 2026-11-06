package com.el_giancar.infrastructure.http

import com.el_giancar.application.ReadinessState
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Two probes with different jobs:
 *
 * - `/healthz` is liveness: the Ktor process is up. Always 200 so a Telegram outage
 *   does not cause a restart loop.
 * - `/readyz` is readiness: the bot is connected to Telegram. Returns 503 until the
 *   first successful `getMe()` and while the periodic connection check fails.
 */
fun Application.healthRoutes(readiness: ReadinessState = ReadinessState()) {
    routing {
        get("/healthz") {
            call.respondText("OK")
        }
        get("/readyz") {
            if (readiness.isReady) {
                call.respondText("READY")
            } else {
                call.respondText("NOT READY", status = HttpStatusCode.ServiceUnavailable)
            }
        }
    }
}
