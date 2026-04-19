package com.franklinharper.concentra.browser.web

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserWebViewClientTest {
    @Test
    fun `page callbacks emit load and navigation events`() {
        val events = mutableListOf<WebViewEvent>()
        val client = BrowserWebViewClient(onEvent = { events.add(it) })

        client.onPageStarted(null, "https://example.com", null)
        client.doUpdateVisitedHistory(null, "https://example.com", false)
        client.onPageFinished(null, "https://example.com")

        assertEquals(
            listOf(
                WebViewEvent.PageLoadStarted(url = "https://example.com"),
                WebViewEvent.NavigationStateChanged(
                    currentUrl = "https://example.com",
                    canGoBack = false,
                ),
                WebViewEvent.PageLoadFinished(
                    currentUrl = "https://example.com",
                    canGoBack = false,
                ),
            ),
            events,
        )
    }
}
