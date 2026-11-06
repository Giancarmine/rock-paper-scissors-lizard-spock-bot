package com.el_giancar.application

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Whether the bot is connected to Telegram and able to serve updates.
 *
 * The Telegram adapter updates this state; the HTTP readiness probe reads it.
 * Liveness (`/healthz`) intentionally does not depend on it, so a Telegram outage
 * does not make the platform restart an otherwise healthy process.
 */
class ReadinessState {
    private val ready = AtomicBoolean(false)

    val isReady: Boolean
        get() = ready.get()

    fun markReady() {
        ready.set(true)
    }

    fun markNotReady() {
        ready.set(false)
    }
}
