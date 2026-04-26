package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.domain.toDisplay

@Composable
fun StatusWidgetContent(status: WhatsAppStatus, context: Context) {
    val display = status.toDisplay()

    GlanceTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12),
        ) {
            Text(
                text = display.label,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
