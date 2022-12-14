package com.el_giancar.plugins

import io.ktor.server.plugins.openapi.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureHTTP() {
    openAPI(path = "openapi")

}
