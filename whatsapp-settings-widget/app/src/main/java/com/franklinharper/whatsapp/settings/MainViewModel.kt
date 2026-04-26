package com.franklinharper.whatsapp.settings

import androidx.lifecycle.ViewModel
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val repository: WhatsAppStatusRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(repository.getStatus()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = MainUiState(repository.getStatus())
    }
}
