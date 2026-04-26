package com.franklinharper.whatsapp.settings

import androidx.lifecycle.ViewModel
import com.franklinharper.whatsapp.settings.domain.DetectionSource
import com.franklinharper.whatsapp.settings.domain.StatusTrackingRepository
import com.franklinharper.whatsapp.settings.monitor.WhatsAppStatusMonitor
import com.franklinharper.whatsapp.settings.presentation.UnrestrictedSessionFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    private val monitor: WhatsAppStatusMonitor,
    private val trackingRepository: StatusTrackingRepository,
    private val sessionFormatter: UnrestrictedSessionFormatter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(refreshState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = refreshState()
    }

    private fun refreshState(): MainUiState {
        val status = monitor.detectAndRecord(DetectionSource.AppResume)
        val now = System.currentTimeMillis()
        return MainUiState(
            status = status,
            unrestrictedSessions = trackingRepository
                .getUnrestrictedSessionsNewestFirst()
                .map { sessionFormatter.format(it, now) },
        )
    }
}
