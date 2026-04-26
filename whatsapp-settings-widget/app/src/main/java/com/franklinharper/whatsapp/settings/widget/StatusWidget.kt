package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatusRepositoryFactory
import com.franklinharper.whatsapp.settings.widget.WidgetStatusState.toStateValue
import com.franklinharper.whatsapp.settings.widget.WidgetStatusState.toWhatsAppStatusOrNull

class StatusWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ensureStatusStateExists(context, id)

        provideContent {
            val status = currentState(WidgetStatusState.StatusKey)
                ?.toWhatsAppStatusOrNull()
                ?: WhatsAppStatusRepositoryFactory.create(context).getStatus()
            StatusWidgetContent(status = status, context = context)
        }
    }

    private suspend fun ensureStatusStateExists(context: Context, id: GlanceId) {
        val state = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        if (state[WidgetStatusState.StatusKey] == null) {
            val status = WhatsAppStatusRepositoryFactory.create(context).getStatus()
            updateAppWidgetState(context, id) { preferences ->
                preferences[WidgetStatusState.StatusKey] = status.toStateValue()
            }
        }
    }
}
