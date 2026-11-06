package com.el_giancar.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpectedLongPollTimeoutFilterTest {
    private val context = LoggerContext()
    private val logger = context.getLogger("KSLog")
    private val filter: TurboFilter = ExpectedLongPollTimeoutFilter()

    @Test
    fun `drops the expected getUpdates timeout`() {
        val timeout =
            HttpRequestTimeoutException(
                "https://api.telegram.org/bot123456789:AAExample/getUpdates",
                30_000,
            )

        assertEquals(FilterReply.DENY, decide(timeout))
    }

    @Test
    fun `drops a wrapped getUpdates timeout`() {
        val timeout =
            HttpRequestTimeoutException(
                "https://api.telegram.org/bot123456789:AAExample/getUpdates",
                30_000,
            )

        assertEquals(FilterReply.DENY, decide(RuntimeException("request failed", timeout)))
    }

    @Test
    fun `keeps timeouts for other requests`() {
        val timeout =
            HttpRequestTimeoutException(
                "https://api.telegram.org/bot123456789:AAExample/sendMessage",
                30_000,
            )

        assertEquals(FilterReply.NEUTRAL, decide(timeout))
    }

    @Test
    fun `keeps unrelated errors`() {
        assertEquals(FilterReply.NEUTRAL, decide(IllegalStateException("boom")))
        assertEquals(FilterReply.NEUTRAL, decide(null))
    }

    private fun decide(throwable: Throwable?) = filter.decide(null, logger, Level.ERROR, "Something web wrong", emptyArray(), throwable)
}
