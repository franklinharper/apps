package com.franklinharper.whatsapp.settings.domain

interface WhatsAppStatusRepository {
    fun getStatus(): WhatsAppStatus
}
