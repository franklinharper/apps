package com.franklinharper.whatsapp.settings.monitor

import com.franklinharper.whatsapp.settings.domain.DetectionSource
import com.franklinharper.whatsapp.settings.domain.StatusTrackingRepository
import com.franklinharper.whatsapp.settings.domain.UnrestrictedSession
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatusRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppStatusMonitorTest {

    private class FakeStatusRepository(private val status: WhatsAppStatus) : WhatsAppStatusRepository {
        override fun getStatus(): WhatsAppStatus = status
    }

    private class FakeTrackingRepository : StatusTrackingRepository {
        var recordedStatus: WhatsAppStatus? = null
        var recordedTimestamp: Long? = null
        var recordedSource: DetectionSource? = null

        override fun recordIfChanged(
            status: WhatsAppStatus,
            timestampMillis: Long,
            source: DetectionSource,
        ) {
            recordedStatus = status
            recordedTimestamp = timestampMillis
            recordedSource = source
        }

        override fun getUnrestrictedSessionsNewestFirst(): List<UnrestrictedSession> = emptyList()
    }

    @Test
    fun `detectAndRecord records detected status with timestamp and source`() {
        val trackingRepository = FakeTrackingRepository()
        val monitor = WhatsAppStatusMonitor(
            statusRepository = FakeStatusRepository(WhatsAppStatus.BackgroundUsageUnrestricted),
            trackingRepository = trackingRepository,
            clock = { 1234L },
        )

        val status = monitor.detectAndRecord(DetectionSource.PeriodicWorker)

        assertEquals(WhatsAppStatus.BackgroundUsageUnrestricted, status)
        assertEquals(WhatsAppStatus.BackgroundUsageUnrestricted, trackingRepository.recordedStatus)
        assertEquals(1234L, trackingRepository.recordedTimestamp)
        assertEquals(DetectionSource.PeriodicWorker, trackingRepository.recordedSource)
    }
}
