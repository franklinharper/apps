package com.franklinharper.whatsapp.settings.domain

import android.content.pm.PackageManager
import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SystemWhatsAppStatusRepositoryTest {

    private val packageManager: PackageManager = mock()
    private val powerManager: PowerManager = mock()

    @Test
    fun `returns NotInstalled when WhatsApp package not found`() {
        whenever(packageManager.getPackageInfo(any<String>(), any<Int>()))
            .thenThrow(PackageManager.NameNotFoundException())
        val repo = SystemWhatsAppStatusRepository(packageManager, powerManager)
        assertEquals(WhatsAppStatus.NotInstalled, repo.getStatus())
    }

    @Test
    fun `returns BackgroundUsageUnrestricted when ignoring battery optimizations`() {
        whenever(packageManager.getPackageInfo(any<String>(), any<Int>()))
            .thenReturn(mock())
        whenever(powerManager.isIgnoringBatteryOptimizations("com.whatsapp"))
            .thenReturn(true)
        val repo = SystemWhatsAppStatusRepository(packageManager, powerManager)
        assertEquals(WhatsAppStatus.BackgroundUsageUnrestricted, repo.getStatus())
    }

    @Test
    fun `returns BackgroundUsageOptimized when not ignoring battery optimizations`() {
        whenever(packageManager.getPackageInfo(any<String>(), any<Int>()))
            .thenReturn(mock())
        whenever(powerManager.isIgnoringBatteryOptimizations("com.whatsapp"))
            .thenReturn(false)
        val repo = SystemWhatsAppStatusRepository(packageManager, powerManager)
        assertEquals(WhatsAppStatus.BackgroundUsageOptimized, repo.getStatus())
    }
}
