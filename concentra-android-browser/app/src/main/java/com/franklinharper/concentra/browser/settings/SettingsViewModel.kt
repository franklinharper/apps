package com.franklinharper.concentra.browser.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            SettingsUiState(
                thirdPartyCookiesEnabled = settingsRepository.load().thirdPartyCookiesEnabled,
            ),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onThirdPartyCookiesChanged(enabled: Boolean) {
        settingsRepository.saveThirdPartyCookiesEnabled(enabled)
        _uiState.value = _uiState.value.copy(thirdPartyCookiesEnabled = enabled)
    }
}

data class SettingsUiState(
    val thirdPartyCookiesEnabled: Boolean = false,
)
