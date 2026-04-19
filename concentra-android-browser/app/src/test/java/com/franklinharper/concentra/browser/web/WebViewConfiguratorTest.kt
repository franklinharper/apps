package com.franklinharper.concentra.browser.web

import com.franklinharper.concentra.browser.settings.BrowserSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewConfiguratorTest {
    @Test
    fun `configurator enables javascript dom storage and zoom`() {
        val sink = FakeWebViewSettingsSink()

        WebViewConfigurator().configure(
            settings = BrowserSettings(thirdPartyCookiesEnabled = true),
            sink = sink,
        )

        assertTrue(sink.javaScriptEnabled)
        assertTrue(sink.domStorageEnabled)
        assertTrue(sink.builtInZoomControlsEnabled)
        assertFalse(sink.displayZoomControlsEnabled)
        assertTrue(sink.supportZoomEnabled)
        assertTrue(sink.firstPartyCookiesEnabled)
        assertFalse(sink.supportMultipleWindows)
        assertTrue(sink.thirdPartyCookiesEnabled)
    }

    @Test
    fun `configurator disables third party cookies by default`() {
        val sink = FakeWebViewSettingsSink()

        WebViewConfigurator().configure(
            settings = BrowserSettings(),
            sink = sink,
        )

        assertFalse(sink.thirdPartyCookiesEnabled)
    }
}

private class FakeWebViewSettingsSink : WebViewConfigurator.SettingsSink {
    override var javaScriptEnabled: Boolean = false
    override var domStorageEnabled: Boolean = false
    override var builtInZoomControlsEnabled: Boolean = false
    override var displayZoomControlsEnabled: Boolean = true
    override var supportZoomEnabled: Boolean = false
    override var supportMultipleWindows: Boolean = true
    override var firstPartyCookiesEnabled: Boolean = false
    override var thirdPartyCookiesEnabled: Boolean = true
}
