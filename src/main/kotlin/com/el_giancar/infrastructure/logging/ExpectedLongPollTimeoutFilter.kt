package com.el_giancar.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import io.ktor.client.plugins.HttpRequestTimeoutException
import org.slf4j.Marker

/**
 * Drops the timeout that tgbotapi logs on every idle long-poll cycle.
 *
 * tgbotapi sets the HTTP request timeout equal to Telegram's `getUpdates` timeout,
 * so an idle bot raises `HttpRequestTimeoutException` about every 30 seconds. The
 * long-polling loop handles that case and retries, but tgbotapi's request executor
 * logs it at ERROR before the loop can suppress it. On a free Render service that is
 * the noisiest line in the logs and hides real failures.
 *
 * Only timeouts whose URL targets `getUpdates` are dropped. Timeouts for other
 * requests, and every other error, still reach the appenders. Registered in
 * `logback.xml`; see `ExpectedLongPollTimeoutFilterTest`.
 */
class ExpectedLongPollTimeoutFilter : TurboFilter() {
    override fun decide(
        marker: Marker?,
        logger: Logger,
        level: Level,
        format: String?,
        params: Array<out Any>?,
        throwable: Throwable?,
    ): FilterReply =
        if (isExpectedLongPollTimeout(throwable)) {
            FilterReply.DENY
        } else {
            FilterReply.NEUTRAL
        }
}

internal fun isExpectedLongPollTimeout(throwable: Throwable?): Boolean {
    var current: Throwable? = throwable
    while (current != null) {
        if (
            current is HttpRequestTimeoutException &&
            current.message?.contains("/getUpdates") == true
        ) {
            return true
        }
        current = current.cause?.takeIf { it !== current }
    }
    return false
}
