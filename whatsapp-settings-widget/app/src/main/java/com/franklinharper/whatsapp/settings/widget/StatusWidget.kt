package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatusRepositoryFactory

class StatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val status = WhatsAppStatusRepositoryFactory.create(context).getStatus()
        provideContent {
            StatusWidgetContent(status = status, context = context)
        }
    }
}
