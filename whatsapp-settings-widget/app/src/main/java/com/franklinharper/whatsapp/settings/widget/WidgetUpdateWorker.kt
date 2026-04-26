package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        StatusWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "widget_periodic_update"
    }
}
