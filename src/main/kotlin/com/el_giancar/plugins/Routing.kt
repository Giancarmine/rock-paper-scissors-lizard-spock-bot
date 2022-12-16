package com.el_giancar.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/healthz") {
            call.respond(HttpStatusCode.OK, "OK")
        }
    }
}
