package com.el_giancar.infrastructure.telegram

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StartPayloadTest {
    @Test
    fun `reads the payload after the command`() {
        assertEquals("share", startPayload("/start share"))
        assertEquals("share", startPayload("  /start   share  "))
    }

    @Test
    fun `ignores the bot username in a group command`() {
        assertEquals("group", startPayload("/start@RockPaperScissorsLizardSpockBot group"))
    }

    @Test
    fun `returns null when there is no payload or a different command`() {
        assertNull(startPayload("/start"))
        assertNull(startPayload("/help share"))
        assertNull(startPayload("hello"))
    }

    @Test
    fun `keeps only the first payload token`() {
        assertEquals("share", startPayload("/start share extra"))
    }

    @Test
    fun `sanitizes campaign tokens for logging`() {
        assertEquals("share", sanitizedCampaign("share"))
        assertEquals("group_2026-11", sanitizedCampaign("group_2026-11"))
        assertEquals("share", sanitizedCampaign("sh are!"))
        assertNull(sanitizedCampaign(null))
        assertNull(sanitizedCampaign(""))
        assertNull(sanitizedCampaign("!!!"))
    }

    @Test
    fun `caps the campaign token length`() {
        assertEquals(64, sanitizedCampaign("a".repeat(200))?.length)
    }
}
