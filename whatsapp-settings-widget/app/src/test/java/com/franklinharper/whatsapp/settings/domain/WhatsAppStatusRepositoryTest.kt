package com.franklinharper.whatsapp.settings.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppStatusRepositoryTest {

    private class FakeRepository(private val status: WhatsAppStatus) : WhatsAppStatusRepository {
        override fun getStatus(): WhatsAppStatus = status
    }

    @Test
    fun `fake returns Unrestricted status`() {
        val repo = FakeRepository(WhatsAppStatus.Unrestricted)
        assertEquals(WhatsAppStatus.Unrestricted, repo.getStatus())
    }

    @Test
    fun `fake returns Optimized status`() {
        val repo = FakeRepository(WhatsAppStatus.Optimized)
        assertEquals(WhatsAppStatus.Optimized, repo.getStatus())
    }

    @Test
    fun `fake returns NotInstalled status`() {
        val repo = FakeRepository(WhatsAppStatus.NotInstalled)
        assertEquals(WhatsAppStatus.NotInstalled, repo.getStatus())
    }
}
