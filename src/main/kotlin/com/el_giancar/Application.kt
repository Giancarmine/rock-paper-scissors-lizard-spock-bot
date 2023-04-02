package com.el_giancar

import com.el_giancar.plugins.configureTelegram
import dev.inmo.micro_utils.coroutines.launchSynchronously
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    launchSynchronously { configureTelegram() }
}
