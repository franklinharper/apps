package com.franklinharper.whatsapp.settings.monitor

import com.franklinharper.whatsapp.settings.domain.DetectionSource
import com.franklinharper.whatsapp.settings.domain.StatusTrackingRepository
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatusRepository

class WhatsAppStatusMonitor(
    private val statusRepository: WhatsAppStatusRepository,
    private val trackingRepository: StatusTrackingRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun detectAndRecord(source: DetectionSource): WhatsAppStatus {
        val status = statusRepository.getStatus()
        trackingRepository.recordIfChanged(
            status = status,
            timestampMillis = clock(),
            source = source,
        )
        return status
    }
}
