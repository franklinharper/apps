package com.franklinharper.concentra.browser.model

import com.franklinharper.concentra.browser.settings.BrowserSettings

data class BrowserUiState(
    val currentUrl: String? = null,
    val pendingUrlInput: String = "",
    val isChromeVisible: Boolean = true,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val isGoogleEnabled: Boolean = true,
    val isArchiveTodayEnabled: Boolean = false,
    val isShareEnabled: Boolean = false,
    val isFindInPageEnabled: Boolean = false,
    val settings: BrowserSettings = BrowserSettings(),
)
