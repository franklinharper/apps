package com.franklinharper.whatsapp.settings.domain

sealed class WhatsAppStatus {
    data object Unrestricted : WhatsAppStatus()
    data object Optimized : WhatsAppStatus()
    data object NotInstalled : WhatsAppStatus()
}
