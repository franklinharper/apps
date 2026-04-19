package com.franklinharper.concentra.browser

import androidx.lifecycle.ViewModel
import com.franklinharper.concentra.browser.model.BrowserAction
import com.franklinharper.concentra.browser.model.BrowserUiState
import com.franklinharper.concentra.browser.model.LaunchRequest
import com.franklinharper.concentra.browser.settings.SettingsRepository
import com.franklinharper.concentra.browser.url.ArchiveTodayUrlBuilder
import com.franklinharper.concentra.browser.url.UrlNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel(
    launchRequest: LaunchRequest,
    settingsRepository: SettingsRepository,
    private val urlNormalizer: UrlNormalizer = UrlNormalizer(),
    private val archiveTodayUrlBuilder: ArchiveTodayUrlBuilder = ArchiveTodayUrlBuilder(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState(launchRequest, settingsRepository))
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private var pendingCommand: WebCommand? =
        when (launchRequest) {
            is LaunchRequest.OpenUrl -> WebCommand.LoadUrl(launchRequest.url)
            LaunchRequest.Empty -> null
        }

    private var pendingEffect: Effect? = null

    fun onAction(action: BrowserAction) {
        when (action) {
            is BrowserAction.SubmitUrl -> loadUrl(urlNormalizer.normalize(action.rawInput))
            BrowserAction.GoogleClicked -> loadUrl(GOOGLE_URL)
            BrowserAction.ArchiveTodayClicked -> {
                val currentUrl = uiState.value.currentUrl ?: return
                loadUrl(archiveTodayUrlBuilder.build(currentUrl))
            }
            BrowserAction.ShareLinkClicked -> {
                val currentUrl = uiState.value.currentUrl ?: return
                pendingEffect = Effect.ShareUrl(currentUrl)
            }
            BrowserAction.FindInPageClicked -> {
                if (uiState.value.currentUrl != null) {
                    pendingEffect = Effect.OpenFindInPage
                }
            }
            BrowserAction.OpenSettingsClicked -> pendingEffect = Effect.OpenSettings
            BrowserAction.ExitClicked -> pendingEffect = Effect.Exit
            BrowserAction.BackPressed -> handleBackPressed()
            BrowserAction.ShowChrome -> updateChromeVisibility(isVisible = true)
            BrowserAction.HideChrome -> updateChromeVisibility(isVisible = false)
        }
    }

    fun consumePendingWebCommand(): String? {
        val command = pendingCommand ?: return null
        pendingCommand = null
        return when (command) {
            is WebCommand.LoadUrl -> command.url
        }
    }

    fun consumePendingEffect(): Effect? {
        val effect = pendingEffect ?: return null
        pendingEffect = null
        return effect
    }

    fun updatePageState(
        currentUrl: String?,
        canGoBack: Boolean = false,
    ) {
        _uiState.value =
            _uiState.value.copy(
                currentUrl = currentUrl,
                canGoBack = canGoBack,
                isArchiveTodayEnabled = currentUrl != null,
                isShareEnabled = currentUrl != null,
                isFindInPageEnabled = currentUrl != null,
            )
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

    private fun loadUrl(url: String) {
        pendingCommand = WebCommand.LoadUrl(url)
        _uiState.value =
            _uiState.value.copy(
                pendingUrlInput = url,
                isChromeVisible = false,
            )
    }

    private fun handleBackPressed() {
        when {
            uiState.value.isChromeVisible -> updateChromeVisibility(isVisible = false)
            uiState.value.canGoBack -> pendingEffect = Effect.GoBack
            else -> pendingEffect = Effect.Exit
        }
    }

    private fun updateChromeVisibility(isVisible: Boolean) {
        _uiState.value = _uiState.value.copy(isChromeVisible = isVisible)
    }

    private sealed interface WebCommand {
        data class LoadUrl(val url: String) : WebCommand
    }

    sealed interface Effect {
        data class ShareUrl(val url: String) : Effect

        data object OpenFindInPage : Effect

        data object OpenSettings : Effect

        data object GoBack : Effect

        data object Exit : Effect
    }

    private companion object {
        const val GOOGLE_URL = "https://www.google.com"
    }
}
