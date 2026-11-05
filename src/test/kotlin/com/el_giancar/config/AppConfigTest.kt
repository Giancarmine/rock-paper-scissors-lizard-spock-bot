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
}
