package com.el_giancar

import com.el_giancar.plugins.configureMonitoring
import com.el_giancar.plugins.configureRouting
import com.el_giancar.plugins.configureSerialization
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
    configureMonitoring()
    configureSerialization()
    configureRouting()

    launchSynchronously { configureTelegram() }
}
