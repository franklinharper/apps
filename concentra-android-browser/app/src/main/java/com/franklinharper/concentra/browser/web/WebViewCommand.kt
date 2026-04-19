package com.franklinharper.concentra.browser.web

sealed interface WebViewCommand {
    data class LoadUrl(val url: String) : WebViewCommand
}
