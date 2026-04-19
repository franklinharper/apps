package com.franklinharper.concentra.browser.web

sealed interface WebViewEvent {
    data class PageLoadStarted(
        val url: String?,
    ) : WebViewEvent

    data class PageLoadFinished(
        val currentUrl: String?,
        val canGoBack: Boolean,
    ) : WebViewEvent

    data class NavigationStateChanged(
        val currentUrl: String?,
        val canGoBack: Boolean,
    ) : WebViewEvent
}
