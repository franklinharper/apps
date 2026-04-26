package com.franklinharper.whatsapp.settings

import android.content.Context
import com.franklinharper.whatsapp.settings.data.StatusTrackingDatabase
import com.franklinharper.whatsapp.settings.domain.StatusTrackingRepository
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatusRepositoryFactory
import com.franklinharper.whatsapp.settings.monitor.WhatsAppStatusMonitor
import com.franklinharper.whatsapp.settings.presentation.UnrestrictedSessionFormatter

object AppDependencies {
    fun statusTrackingRepository(context: Context): StatusTrackingRepository =
        StatusTrackingDatabase.getInstance(context)

    fun statusMonitor(context: Context): WhatsAppStatusMonitor = WhatsAppStatusMonitor(
        statusRepository = WhatsAppStatusRepositoryFactory.create(context),
        trackingRepository = statusTrackingRepository(context),
    )

    fun unrestrictedSessionFormatter(): UnrestrictedSessionFormatter = UnrestrictedSessionFormatter()
}
