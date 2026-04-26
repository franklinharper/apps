package com.franklinharper.whatsapp.settings.domain

import android.content.pm.PackageManager
import android.os.PowerManager

class SystemWhatsAppStatusRepository(
    private val packageManager: PackageManager,
    private val powerManager: PowerManager,
) : WhatsAppStatusRepository {

    override fun getStatus(): WhatsAppStatus {
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo("com.whatsapp", 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return WhatsAppStatus.NotInstalled
        }
        return if (powerManager.isIgnoringBatteryOptimizations("com.whatsapp")) {
            WhatsAppStatus.Unrestricted
        } else {
            WhatsAppStatus.Optimized
        }
    }
}
