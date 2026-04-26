package com.franklinharper.whatsapp.settings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppStatusRepositoryTest {

    private class FakeRepository(private val status: WhatsAppStatus) : WhatsAppStatusRepository {
        override fun getStatus(): WhatsAppStatus = status
    }

    @Test
    fun `fake returns BackgroundUsageUnrestricted status`() {
        val repo = FakeRepository(WhatsAppStatus.BackgroundUsageUnrestricted)
        assertEquals(WhatsAppStatus.BackgroundUsageUnrestricted, repo.getStatus())
    }

    @Test
    fun `fake returns BackgroundUsageRestrictedOrOptimized status`() {
        val repo = FakeRepository(WhatsAppStatus.BackgroundUsageRestrictedOrOptimized)
        assertEquals(WhatsAppStatus.BackgroundUsageRestrictedOrOptimized, repo.getStatus())
    }

    @Test
    fun `fake returns NotInstalled status`() {
        val repo = FakeRepository(WhatsAppStatus.NotInstalled)
        assertEquals(WhatsAppStatus.NotInstalled, repo.getStatus())
    }
}
