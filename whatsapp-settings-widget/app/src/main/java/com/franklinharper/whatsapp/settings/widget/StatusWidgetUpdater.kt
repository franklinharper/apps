package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.franklinharper.whatsapp.settings.AppDependencies
import com.franklinharper.whatsapp.settings.domain.DetectionSource
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.widget.WidgetStatusState.toStateValue

object StatusWidgetUpdater {
    suspend fun refresh(context: Context) {
        refresh(
            context = context,
            status = AppDependencies.statusMonitor(context).detectAndRecord(DetectionSource.WidgetInitialRender),
        )
    }

    suspend fun refresh(context: Context, status: WhatsAppStatus) {
        val widget = StatusWidget()
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(StatusWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { preferences ->
                preferences[WidgetStatusState.StatusKey] = status.toStateValue()
            }
        }
        widget.updateAll(context)
    }
}
