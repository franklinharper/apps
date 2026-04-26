package com.franklinharper.whatsapp.settings.domain

sealed class WhatsAppStatus {
    data object BackgroundUsageUnrestricted : WhatsAppStatus()
    data object BackgroundUsageRestrictedOrOptimized : WhatsAppStatus()
    data object NotInstalled : WhatsAppStatus()
}
