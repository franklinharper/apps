package com.franklinharper.concentra.browser

import androidx.lifecycle.ViewModel
import com.franklinharper.concentra.browser.model.BrowserAction
import com.franklinharper.concentra.browser.model.BrowserUiState
import com.franklinharper.concentra.browser.model.LaunchRequest
import com.franklinharper.concentra.browser.settings.SettingsRepository
import com.franklinharper.concentra.browser.url.ArchiveTodayUrlBuilder
import com.franklinharper.concentra.browser.url.UrlNormalizer
import com.franklinharper.concentra.browser.web.WebViewCommand
import com.franklinharper.concentra.browser.web.WebViewEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel(
    launchRequest: LaunchRequest,
    private val settingsRepository: SettingsRepository,
    private val urlNormalizer: UrlNormalizer = UrlNormalizer(),
    private val archiveTodayUrlBuilder: ArchiveTodayUrlBuilder = ArchiveTodayUrlBuilder(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState(launchRequest, settingsRepository))
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private var pendingCommand: WebViewCommand? =
        when (launchRequest) {
            is LaunchRequest.OpenUrl -> WebViewCommand.LoadUrl(launchRequest.url)
            LaunchRequest.Empty -> null
        }

    private val _pendingEffect = MutableStateFlow<Effect?>(null)
    val pendingEffect: StateFlow<Effect?> = _pendingEffect.asStateFlow()

    fun onAction(action: BrowserAction) {
        when (action) {
            is BrowserAction.SubmitUrl -> handleSubmitUrl(action.rawInput)
            BrowserAction.GoogleClicked -> loadUrl(GOOGLE_URL)
            BrowserAction.ArchiveTodayClicked -> {
                val currentUrl = uiState.value.currentUrl ?: return
                loadUrl(archiveTodayUrlBuilder.build(currentUrl))
            }
            BrowserAction.ShareLinkClicked -> {
                val currentUrl = uiState.value.currentUrl ?: return
                _pendingEffect.value = Effect.ShareUrl(currentUrl)
            }
            BrowserAction.FindInPageClicked -> {
                if (uiState.value.currentUrl != null) {
                    _pendingEffect.value = Effect.OpenFindInPage
                }
            }
            BrowserAction.OpenSettingsClicked -> _pendingEffect.value = Effect.OpenSettings
            BrowserAction.ExitClicked -> _pendingEffect.value = Effect.Exit
            BrowserAction.BackPressed -> handleBackPressed()
            BrowserAction.ShowChrome -> updateChromeVisibility(isVisible = true)
            BrowserAction.HideChrome -> updateChromeVisibility(isVisible = false)
        }
    }

    private fun handleSubmitUrl(rawInput: String) {
        if (rawInput.isBlank()) {
            return
        }

        loadUrl(urlNormalizer.normalize(rawInput))
    }

    fun onWebViewEvent(event: WebViewEvent) {
        when (event) {
            is WebViewEvent.PageLoadStarted -> {
                _uiState.value =
                    _uiState.value.copy(
                        currentUrl = event.url,
                        isLoading = true,
                        isArchiveTodayEnabled = event.url != null,
                        isShareEnabled = event.url != null,
                        isFindInPageEnabled = event.url != null,
                    )
            }
            is WebViewEvent.PageLoadFinished ->
                applyNavigationState(
                    currentUrl = event.currentUrl,
                    canGoBack = event.canGoBack,
                    isLoading = false,
                )
            is WebViewEvent.NavigationStateChanged ->
                applyNavigationState(
                    currentUrl = event.currentUrl,
                    canGoBack = event.canGoBack,
                    isLoading = _uiState.value.isLoading,
                )
        }
    }

    fun consumePendingWebCommand(): WebViewCommand? {
        val command = pendingCommand ?: return null
        pendingCommand = null
        return command
    }

    fun consumePendingEffect(): Effect? {
        val effect = _pendingEffect.value ?: return null
        _pendingEffect.value = null
        return effect
    }

    fun reloadSettings() {
        _uiState.value = _uiState.value.copy(settings = settingsRepository.load())
    }

    private fun applyNavigationState(
        currentUrl: String?,
        canGoBack: Boolean,
        isLoading: Boolean,
    ) {
        _uiState.value =
            _uiState.value.copy(
                currentUrl = currentUrl,
                canGoBack = canGoBack,
                isLoading = isLoading,
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
        pendingCommand = WebViewCommand.LoadUrl(url)
        _uiState.value =
            _uiState.value.copy(
                pendingUrlInput = url,
                isLoading = true,
                isChromeVisible = false,
            )
    }

    private fun handleBackPressed() {
        when {
            uiState.value.isChromeVisible && uiState.value.currentUrl == null ->
                _pendingEffect.value = Effect.Exit
            uiState.value.isChromeVisible -> updateChromeVisibility(isVisible = false)
            uiState.value.canGoBack -> _pendingEffect.value = Effect.GoBack
            else -> _pendingEffect.value = Effect.Exit
        }
    }

    private fun updateChromeVisibility(isVisible: Boolean) {
        _uiState.value = _uiState.value.copy(isChromeVisible = isVisible)
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
