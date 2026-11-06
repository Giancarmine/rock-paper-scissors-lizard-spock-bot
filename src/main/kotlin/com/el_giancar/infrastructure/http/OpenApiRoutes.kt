package com.el_giancar.infrastructure.http

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val OPENAPI_RESOURCE = "openapi/documentation.yaml"

/**
 * Serves the OpenAPI document that ships in the jar, so tools and humans can fetch
 * it from a running instance instead of reading the source tree.
 *
 * The resource is read once at startup and cached in memory. The document is static,
 * so re-reading it on every request would only add I/O.
 */
fun Application.openApiRoutes(document: String = loadOpenApiDocument()) {
    routing {
        get("/openapi.yaml") {
            call.respondText(document, ContentType.parse("application/yaml"))
        }
    }
}

internal fun loadOpenApiDocument(): String {
    val resource =
        requireNotNull(
            Thread.currentThread().contextClassLoader.getResource(OPENAPI_RESOURCE),
        ) {
            "Missing OpenAPI resource on the classpath: $OPENAPI_RESOURCE"
        }
    return resource.readText()
}
