package com.franklinharper.concentra.browser.web

import com.franklinharper.concentra.browser.settings.BrowserSettings

class WebViewConfigurator {
    fun configure(
        settings: BrowserSettings,
        sink: SettingsSink,
    ) {
        sink.javaScriptEnabled = true
        sink.domStorageEnabled = true
        sink.builtInZoomControlsEnabled = true
        sink.displayZoomControlsEnabled = false
        sink.supportZoomEnabled = true
        sink.firstPartyCookiesEnabled = true
        sink.thirdPartyCookiesEnabled = settings.thirdPartyCookiesEnabled
        sink.supportMultipleWindows = false
    }

    interface SettingsSink {
        var javaScriptEnabled: Boolean
        var domStorageEnabled: Boolean
        var builtInZoomControlsEnabled: Boolean
        var displayZoomControlsEnabled: Boolean
        var supportZoomEnabled: Boolean
        var supportMultipleWindows: Boolean
        var firstPartyCookiesEnabled: Boolean
        var thirdPartyCookiesEnabled: Boolean
    }
}
