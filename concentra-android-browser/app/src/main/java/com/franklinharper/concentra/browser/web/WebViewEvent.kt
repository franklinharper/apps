package com.franklinharper.concentra.browser.web

sealed interface WebViewEvent {
    data class NavigationStateChanged(
        val currentUrl: String?,
        val canGoBack: Boolean,
    ) : WebViewEvent
}
