package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.franklinharper.whatsapp.settings.domain.SystemWhatsAppStatusRepository

class StatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            Log.d(TAG, "provideGlance started")
            val repository = SystemWhatsAppStatusRepository(
                packageManager = context.packageManager,
                powerManager = context.getSystemService(PowerManager::class.java),
            )
            val status = repository.getStatus()
            Log.d(TAG, "status = $status")
            provideContent {
                Log.d(TAG, "provideContent composing")
                StatusWidgetContent(status = status, context = context)
                Log.d(TAG, "provideContent done")
            }
            Log.d(TAG, "provideGlance finished")
        } catch (e: Exception) {
            Log.e(TAG, "provideGlance failed", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "StatusWidget"
    }
}
