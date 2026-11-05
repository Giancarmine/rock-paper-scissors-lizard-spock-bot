package com.el_giancar.infrastructure.logging

import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.logging.Logger

/**
 * Routes `java.util.logging` records through SLF4J/Logback.
 *
 * tgbotapi logs through kslog, whose JVM implementation uses `java.util.logging`
 * directly. Those records bypass Logback's pattern, so tokens in Telegram request
 * URLs were written to the platform logs in plaintext. Installing this bridge makes
 * every JUL record go through `logback.xml`, where `bot<id>:<secret>` is redacted.
 *
 * Call this once, before creating the Telegram bot.
 */
fun installJulToSlf4jBridge() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    // kslog logs at INFO and above; the JUL root level would otherwise drop them.
    Logger.getLogger("KSLog").level = java.util.logging.Level.ALL
}
