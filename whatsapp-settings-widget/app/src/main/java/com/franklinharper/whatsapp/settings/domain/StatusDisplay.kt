package com.franklinharper.whatsapp.settings.domain

/**
 * Single source of truth for how a [WhatsAppStatus] is displayed in the app UI and widget.
 */
data class StatusDisplay(
    val label: String,
    val enabled: Boolean,
)

fun WhatsAppStatus.toDisplay(): StatusDisplay = when (this) {
    WhatsAppStatus.BackgroundUsageUnrestricted -> StatusDisplay(
        label = "Background usage: Unrestricted",
        enabled = true,
    )
    WhatsAppStatus.BackgroundUsageUnknown -> StatusDisplay(
        label = "Background usage: Not unrestricted",
        enabled = false,
    )
    WhatsAppStatus.NotInstalled -> StatusDisplay(
        label = "WhatsApp not installed",
        enabled = false,
    )
}
