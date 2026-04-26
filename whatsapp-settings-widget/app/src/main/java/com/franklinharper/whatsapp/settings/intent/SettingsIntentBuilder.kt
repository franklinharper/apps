package com.franklinharper.whatsapp.settings.intent

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.franklinharper.whatsapp.settings.domain.WhatsAppPackage

data class SettingsIntentSpec(
    val action: String,
    val packageName: String,
)

class SettingsIntentBuilder {

    fun spec(): SettingsIntentSpec = SettingsIntentSpec(
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        packageName = WhatsAppPackage.REGULAR,
    )

    fun build(): Intent {
        val spec = spec()
        return Intent(
            spec.action,
            Uri.fromParts("package", spec.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
