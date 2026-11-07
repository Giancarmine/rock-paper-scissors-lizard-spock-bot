package com.el_giancar.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigTest {
    @Test
    fun `reads the token and defaults the port`() {
        val config = AppConfig.fromEnvironment(mapOf("BOT_TOKEN" to "token"))

        assertEquals("token", config.botToken)
        assertEquals(8080, config.port)
    }

    @Test
    fun `reads an explicit port`() {
        val config = AppConfig.fromEnvironment(mapOf("BOT_TOKEN" to "token", "PORT" to "9090"))

        assertEquals(9090, config.port)
    }

    @Test
    fun `fails fast when the token is missing`() {
        assertFailsWith<IllegalStateException> {
            AppConfig.fromEnvironment(emptyMap())
        }
    }

    @Test
    fun `reads optional database settings`() {
        val config =
            AppConfig.fromEnvironment(
                mapOf(
                    "BOT_TOKEN" to "token",
                    "DATABASE_URL" to "postgresql://db.example.com/neondb",
                    "DATABASE_USER" to "alice",
                    "DATABASE_PASSWORD" to "secret",
                ),
            )

        assertEquals("postgresql://db.example.com/neondb", config.databaseUrl)
        assertEquals("alice", config.databaseUser)
        assertEquals("secret", config.databasePassword)
    }

    @Test
    fun `treats blank database settings as absent`() {
        val config =
            AppConfig.fromEnvironment(
                mapOf(
                    "BOT_TOKEN" to "token",
                    "DATABASE_URL" to "  ",
                    "DATABASE_USER" to "",
                ),
            )

        assertEquals(null, config.databaseUrl)
        assertEquals(null, config.databaseUser)
        assertEquals(null, config.databasePassword)
    }
}
