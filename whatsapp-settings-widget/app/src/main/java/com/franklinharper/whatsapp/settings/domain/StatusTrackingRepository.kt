package com.franklinharper.whatsapp.settings.domain

interface StatusTrackingRepository {
    fun recordIfChanged(
        status: WhatsAppStatus,
        timestampMillis: Long,
        source: DetectionSource,
    )

    fun getUnrestrictedSessionsNewestFirst(): List<UnrestrictedSession>
}
