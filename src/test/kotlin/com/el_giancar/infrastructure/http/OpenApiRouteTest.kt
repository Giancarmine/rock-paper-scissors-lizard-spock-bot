package com.el_giancar.infrastructure.http

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenApiRouteTest {
    @Test
    fun `openapi yaml serves the bundled document`() =
        testApplication {
            application {
                openApiRoutes()
            }

            val response = client.get("/openapi.yaml")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                response.contentType()?.match(ContentType.parse("application/yaml")) == true,
                "Expected an application/yaml response, got ${response.contentType()}",
            )
            val body = response.bodyAsText()
            assertTrue(body.contains("openapi:"), "Missing OpenAPI version declaration")
            assertTrue(body.contains("/healthz"), "Missing the liveness path")
            assertTrue(body.contains("/readyz"), "Missing the readiness path")
            assertTrue(body.contains("/openapi.yaml"), "The document should describe itself")
        }

    @Test
    fun `openapi yaml serves an injected document`() =
        testApplication {
            application {
                openApiRoutes("# test document")
            }

            val response = client.get("/openapi.yaml")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("# test document", response.bodyAsText())
        }
}
