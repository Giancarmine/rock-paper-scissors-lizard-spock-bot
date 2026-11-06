package com.el_giancar.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.ConsoleAppender
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogbackConfigTest {
    @Test
    fun `redacts bot tokens from messages and stack traces`() {
        val context = LoggerContext()
        try {
            val configurator = JoranConfigurator()
            configurator.context = context
            context.reset()
            val config = requireNotNull(javaClass.classLoader.getResource("logback.xml"))
            configurator.doConfigure(config)

            val root = context.getLogger(Logger.ROOT_LOGGER_NAME)

            @Suppress("UNCHECKED_CAST")
            val appender = root.getAppender("STDOUT") as ConsoleAppender<ILoggingEvent>
            val encoder = appender.encoder as PatternLayoutEncoder

            val token = "123456789:AAExampleTokenValueUsedOnlyForThisTest"
            val url = "https://api.telegram.org/bot$token/getUpdates"
            val event =
                LoggingEvent(
                    LogbackConfigTest::class.java.name,
                    context.getLogger("KTgBot"),
                    Level.ERROR,
                    "Call failed: $url",
                    IllegalStateException("Request failed: $url"),
                    null,
                )

            val output = String(encoder.encode(event), Charsets.UTF_8)

            assertFalse(output.contains(token), "The token leaked into the log output: $output")
            assertTrue(output.contains("bot<redacted>"), "Expected a redacted URL: $output")
        } finally {
            context.stop()
        }
    }

    @Test
    fun `registers the idle long poll timeout filter`() {
        val context = LoggerContext()
        try {
            val configurator = JoranConfigurator()
            configurator.context = context
            context.reset()
            val config = requireNotNull(javaClass.classLoader.getResource("logback.xml"))
            configurator.doConfigure(config)

            assertTrue(
                context.turboFilterList.any { it is ExpectedLongPollTimeoutFilter },
                "logback.xml must register ExpectedLongPollTimeoutFilter",
            )
        } finally {
            context.stop()
        }
    }
}
