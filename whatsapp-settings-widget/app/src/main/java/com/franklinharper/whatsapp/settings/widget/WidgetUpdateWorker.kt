package com.franklinharper.whatsapp.settings.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.franklinharper.whatsapp.settings.AppDependencies
import com.franklinharper.whatsapp.settings.domain.DetectionSource

class WidgetUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val status = AppDependencies.statusMonitor(applicationContext)
            .detectAndRecord(DetectionSource.PeriodicWorker)
        StatusWidgetUpdater.refresh(applicationContext, status)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "widget_periodic_update"
    }
}
