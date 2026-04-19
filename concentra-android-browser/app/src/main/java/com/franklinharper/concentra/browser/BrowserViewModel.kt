package com.franklinharper.concentra.browser

import androidx.lifecycle.ViewModel
import com.franklinharper.concentra.browser.model.BrowserAction
import com.franklinharper.concentra.browser.model.BrowserUiState
import com.franklinharper.concentra.browser.model.LaunchRequest
import com.franklinharper.concentra.browser.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel(
    launchRequest: LaunchRequest,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState(launchRequest, settingsRepository))
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private var pendingCommand: WebCommand? =
        when (launchRequest) {
            is LaunchRequest.OpenUrl -> WebCommand.LoadUrl(launchRequest.url)
            LaunchRequest.Empty -> null
        }

    fun onAction(action: BrowserAction) = Unit

    fun consumePendingWebCommand(): String? {
        val command = pendingCommand ?: return null
        pendingCommand = null
        return when (command) {
            is WebCommand.LoadUrl -> command.url
        }
    }

    private fun initialState(
        launchRequest: LaunchRequest,
        settingsRepository: SettingsRepository,
    ): BrowserUiState {
        val initialUrl =
            when (launchRequest) {
                is LaunchRequest.OpenUrl -> launchRequest.url
                LaunchRequest.Empty -> null
            }

        return BrowserUiState(
            currentUrl = null,
            pendingUrlInput = initialUrl.orEmpty(),
            isChromeVisible = initialUrl == null,
            settings = settingsRepository.load(),
        )
    }

    private sealed interface WebCommand {
        data class LoadUrl(val url: String) : WebCommand
    }
}
