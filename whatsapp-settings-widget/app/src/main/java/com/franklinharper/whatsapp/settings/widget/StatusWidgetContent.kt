package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus

@Composable
fun StatusWidgetContent(status: WhatsAppStatus, context: Context) {
    val statusText = when (status) {
        WhatsAppStatus.BackgroundUsageUnrestricted -> "Background usage: Unrestricted"
        WhatsAppStatus.BackgroundUsageUnknown -> "Background usage: Optimized or Off"
        WhatsAppStatus.NotInstalled -> "WhatsApp not installed"
    }
    GlanceTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxSize(),
        ) {
            Text(
                text = "WhatsApp: $statusText",
                style = TextStyle(color = ColorProvider(GlanceTheme.colors.onSurface)),
            )
        }
    }
}
