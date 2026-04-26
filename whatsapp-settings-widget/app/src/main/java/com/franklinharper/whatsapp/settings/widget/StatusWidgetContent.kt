package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.domain.toDisplay
import com.franklinharper.whatsapp.settings.ui.StatusDisabledColor
import com.franklinharper.whatsapp.settings.ui.StatusEnabledColor
import com.franklinharper.whatsapp.settings.ui.StatusTextColor

@Composable
fun StatusWidgetContent(status: WhatsAppStatus, context: Context) {
    val display = status.toDisplay()
    val bgColor = if (display.enabled) StatusEnabledColor else StatusDisabledColor

    GlanceTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(bgColor))
                .padding(12),
        ) {
            Text(
                text = display.label,
                style = TextStyle(
                    color = ColorProvider(StatusTextColor),
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
