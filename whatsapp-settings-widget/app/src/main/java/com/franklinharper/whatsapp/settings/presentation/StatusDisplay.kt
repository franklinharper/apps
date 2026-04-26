package com.franklinharper.whatsapp.settings.presentation

import androidx.annotation.StringRes
import com.franklinharper.whatsapp.settings.R
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus

data class StatusDisplay(
    @StringRes val labelRes: Int,
    val unrestricted: Boolean,
)

fun WhatsAppStatus.toDisplay(): StatusDisplay = when (this) {
    WhatsAppStatus.BackgroundUsageUnrestricted -> StatusDisplay(
        labelRes = R.string.status_background_usage_unrestricted,
        unrestricted = true,
    )
    WhatsAppStatus.BackgroundUsageRestrictedOrOptimized -> StatusDisplay(
        labelRes = R.string.status_background_usage_restricted_or_optimized,
        unrestricted = false,
    )
    WhatsAppStatus.NotInstalled -> StatusDisplay(
        labelRes = R.string.status_not_installed,
        unrestricted = false,
    )
}
