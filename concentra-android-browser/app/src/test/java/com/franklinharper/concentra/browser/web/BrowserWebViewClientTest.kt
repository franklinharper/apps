package com.franklinharper.concentra.browser.web

import android.webkit.WebView
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

    @Test
    fun `onPageStarted invokes onPageStarted callback with the WebView`() {
        val capturedViews = mutableListOf<WebView?>()
        val client = BrowserWebViewClient(
            onEvent = {},
            onPageStarted = { view -> capturedViews.add(view) },
        )
        val mockView = null  // WebView is hard to instantiate in unit tests; null is valid per the override signature

        client.onPageStarted(mockView, "https://example.com", null)

        assertEquals(1, capturedViews.size)
        assertEquals(mockView, capturedViews.first())
    }
}
