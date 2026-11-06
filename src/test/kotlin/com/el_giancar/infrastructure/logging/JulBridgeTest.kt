package com.el_giancar.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.read.ListAppender
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JulBridgeTest {
    @Test
    fun `redacts tokens logged through java util logging`() {
        installJulToSlf4jBridge()

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val root = context.getLogger(ROOT_LOGGER_NAME)
        root.level = Level.INFO

        val appender =
            ListAppender<ILoggingEvent>().apply {
                this.context = context
                start()
            }
        root.addAppender(appender)

        val token = "123456789:AAExampleTokenValueUsedOnlyForThisTest"
        val url = "https://api.telegram.org/bot$token/getUpdates"

        try {
            Logger.getLogger("KSLog").severe("Request failed: $url")
        } finally {
            root.detachAppender(appender)
        }

        val event = appender.list.last { it.formattedMessage.contains("/getUpdates") }

        @Suppress("UNCHECKED_CAST")
        val encoder =
            (root.getAppender("STDOUT") as ConsoleAppender<ILoggingEvent>)
                .encoder as PatternLayoutEncoder
        val output = String(encoder.encode(event), Charsets.UTF_8)

        assertFalse(output.contains(token), "The token leaked into the log output: $output")
        assertTrue(output.contains("bot<redacted>"), "Expected a redacted URL: $output")
    }
}
