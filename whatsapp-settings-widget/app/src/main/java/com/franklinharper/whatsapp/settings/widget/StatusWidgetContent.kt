package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.franklinharper.whatsapp.settings.WidgetTrampolineActivity
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus

@Composable
fun StatusWidgetContent(status: WhatsAppStatus, context: Context) {
    val (statusText, bgColor) = when (status) {
        WhatsAppStatus.BackgroundUsageUnrestricted ->
            "Background usage: Enabled" to Color.Red
        WhatsAppStatus.BackgroundUsageUnknown ->
            "Background usage: Disabled" to Color.Green
        WhatsAppStatus.NotInstalled ->
            "WhatsApp not installed" to Color.Gray
    }

    GlanceTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(12),
        ) {
            Text(
                text = statusText,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
