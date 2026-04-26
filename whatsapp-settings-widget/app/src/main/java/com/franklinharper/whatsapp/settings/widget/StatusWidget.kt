package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import android.os.PowerManager
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.franklinharper.whatsapp.settings.domain.SystemWhatsAppStatusRepository

class StatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = SystemWhatsAppStatusRepository(
            packageManager = context.packageManager,
            powerManager = context.getSystemService(PowerManager::class.java),
        )
        val status = repository.getStatus()
        provideContent {
            StatusWidgetContent(status = status, context = context)
        }
    }
}
