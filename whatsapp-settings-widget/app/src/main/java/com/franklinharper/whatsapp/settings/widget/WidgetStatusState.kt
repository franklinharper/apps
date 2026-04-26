package com.franklinharper.whatsapp.settings.widget

import androidx.datastore.preferences.core.stringPreferencesKey
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus

object WidgetStatusState {
    val StatusKey = stringPreferencesKey("whatsapp_status")

    fun WhatsAppStatus.toStateValue(): String = when (this) {
        WhatsAppStatus.BackgroundUsageUnrestricted -> "background_usage_unrestricted"
        WhatsAppStatus.BackgroundUsageRestrictedOrOptimized -> "background_usage_restricted_or_optimized"
        WhatsAppStatus.NotInstalled -> "not_installed"
    }

    fun String.toWhatsAppStatusOrNull(): WhatsAppStatus? = when (this) {
        "background_usage_unrestricted" -> WhatsAppStatus.BackgroundUsageUnrestricted
        "background_usage_restricted_or_optimized" -> WhatsAppStatus.BackgroundUsageRestrictedOrOptimized
        "not_installed" -> WhatsAppStatus.NotInstalled
        else -> null
    }
}
