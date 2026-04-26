package com.franklinharper.whatsapp.settings.domain

data class UnrestrictedSession(
    val id: Long,
    val startTimestampMillis: Long,
    val endTimestampMillis: Long?,
)
