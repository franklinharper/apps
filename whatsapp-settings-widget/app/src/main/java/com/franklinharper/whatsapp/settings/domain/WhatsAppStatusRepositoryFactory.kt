package com.franklinharper.whatsapp.settings.domain

import android.content.Context
import android.os.PowerManager

object WhatsAppStatusRepositoryFactory {
    fun create(context: Context): WhatsAppStatusRepository = SystemWhatsAppStatusRepository(
        packageManager = context.packageManager,
        powerManager = context.getSystemService(PowerManager::class.java),
    )
}
