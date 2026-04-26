package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.franklinharper.whatsapp.settings.WidgetTrampolineActivity
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus

@Composable
fun StatusWidgetContent(status: WhatsAppStatus, context: Context) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF128C7E))
                .cornerRadius(16),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.padding(12),
            ) {
                Text(
                    text = "WhatsApp",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Spacer(modifier = GlanceModifier.height(8))

                val (statusText, statusColor) = when (status) {
                    WhatsAppStatus.BackgroundUsageUnrestricted ->
                        "Background usage: Unrestricted" to Color(0xFF25D366)
                    WhatsAppStatus.BackgroundUsageUnknown ->
                        "Background usage: Optimized or Off" to Color(0xFFFFA726)
                    WhatsAppStatus.NotInstalled ->
                        "Not installed" to Color(0xFF9E9E9E)
                }

                Text(
                    text = statusText,
                    style = TextStyle(
                        color = ColorProvider(statusColor),
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Spacer(modifier = GlanceModifier.height(8))

                if (status != WhatsAppStatus.NotInstalled) {
                    Button(
                        text = "Open Settings",
                        onClick = actionStartActivity<WidgetTrampolineActivity>(),
                    )
                }
            }
        }
    }
}
