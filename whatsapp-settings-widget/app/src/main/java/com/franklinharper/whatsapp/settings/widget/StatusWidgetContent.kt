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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.franklinharper.whatsapp.settings.MainActivity
import com.franklinharper.whatsapp.settings.R
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.presentation.toDisplay
import com.franklinharper.whatsapp.settings.ui.StatusDisabledColor
import com.franklinharper.whatsapp.settings.ui.StatusEnabledColor
import com.franklinharper.whatsapp.settings.ui.StatusNotInstalledColor
import com.franklinharper.whatsapp.settings.ui.StatusTextColor

@Composable
fun StatusWidgetContent(status: WhatsAppStatus, context: Context) {
    val display = status.toDisplay()
    val backgroundColor = when (status) {
        WhatsAppStatus.NotInstalled -> StatusNotInstalledColor
        else -> if (display.enabled) StatusEnabledColor else StatusDisabledColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(actionStartActivity<MainActivity>())
            .padding(6.dp),
    ) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(
                color = ColorProvider(StatusTextColor),
                fontWeight = FontWeight.Bold,
            ),
        )

        Spacer(modifier = GlanceModifier.height(2.dp))

        Image(
            provider = ImageProvider(R.drawable.ic_battery_widget),
            contentDescription = context.getString(display.labelRes),
            modifier = GlanceModifier.size(38.dp),
        )
    }
}
