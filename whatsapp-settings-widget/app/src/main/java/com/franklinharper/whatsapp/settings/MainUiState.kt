package com.franklinharper.whatsapp.settings

import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.presentation.UnrestrictedSessionUiModel

data class MainUiState(
    val status: WhatsAppStatus,
    val unrestrictedSessions: List<UnrestrictedSessionUiModel> = emptyList(),
)
