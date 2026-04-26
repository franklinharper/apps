package com.franklinharper.whatsapp.settings.presentation

import com.franklinharper.whatsapp.settings.R
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusDisplayTest {

    @Test
    fun `unrestricted maps to enabled display`() {
        val display = WhatsAppStatus.BackgroundUsageUnrestricted.toDisplay()

        assertEquals(R.string.status_background_usage_unrestricted, display.labelRes)
        assertTrue(display.unrestricted)
    }

    @Test
    fun `restricted or optimized maps to disabled display`() {
        val display = WhatsAppStatus.BackgroundUsageRestrictedOrOptimized.toDisplay()

        assertEquals(R.string.status_background_usage_restricted_or_optimized, display.labelRes)
        assertFalse(display.unrestricted)
    }

    @Test
    fun `not installed maps to disabled display`() {
        val display = WhatsAppStatus.NotInstalled.toDisplay()

        assertEquals(R.string.status_not_installed, display.labelRes)
        assertFalse(display.unrestricted)
    }
}
