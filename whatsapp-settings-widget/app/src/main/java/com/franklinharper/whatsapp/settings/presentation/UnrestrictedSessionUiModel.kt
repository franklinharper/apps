package com.franklinharper.whatsapp.settings.presentation

data class UnrestrictedSessionUiModel(
    val id: Long,
    val header: String,
    val timeRange: String,
    val duration: String,
    val ongoing: Boolean,
)
