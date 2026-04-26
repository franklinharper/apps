package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import com.franklinharper.whatsapp.settings.MainActivity
import com.franklinharper.whatsapp.settings.R
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.domain.toDisplay
import com.franklinharper.whatsapp.settings.ui.StatusDisabledColor
import com.franklinharper.whatsapp.settings.ui.StatusEnabledColor
import com.franklinharper.whatsapp.settings.ui.StatusNotInstalledColor

@Composable
fun StatusWidgetContent(status: WhatsAppStatus, context: Context) {
    val display = status.toDisplay()
    val backgroundColor = when (status) {
        WhatsAppStatus.NotInstalled -> StatusNotInstalledColor
        else -> if (display.enabled) StatusEnabledColor else StatusDisabledColor
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp),
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_battery_widget),
            contentDescription = display.label,
        )
    }
}
