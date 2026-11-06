package com.el_giancar.infrastructure.http

import com.el_giancar.application.ReadinessState
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun `healthz responds with OK`() = testApplication {
        application {
            healthRoutes(ReadinessState())
        }

        val response = client.get("/healthz")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun `readyz responds with 503 before the bot is connected`() = testApplication {
        application {
            healthRoutes(ReadinessState())
        }

        val response = client.get("/readyz")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("NOT READY", response.bodyAsText())
    }

    @Test
    fun `readyz responds with 200 once the bot is connected`() = testApplication {
        application {
            healthRoutes(ReadinessState().apply { markReady() })
        }

        val response = client.get("/readyz")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("READY", response.bodyAsText())
    }

    @Test
    fun `readyz returns to 503 when the connection is lost`() = testApplication {
        val readiness = ReadinessState().apply { markReady() }
        application {
            healthRoutes(readiness)
        }

        readiness.markNotReady()
        val response = client.get("/readyz")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }
}
