package com.franklinharper.whatsapp.settings.domain

sealed class WhatsAppStatus {
    data object BackgroundUsageUnrestricted : WhatsAppStatus()
    data object BackgroundUsageUnknown : WhatsAppStatus()
    data object NotInstalled : WhatsAppStatus()
}
