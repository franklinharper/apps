package com.franklinharper.concentra.browser.web

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserWebViewClient(
    private val onEvent: (WebViewEvent) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        onEvent(WebViewEvent.PageLoadStarted(url = url))
    }

    override fun doUpdateVisitedHistory(
        view: WebView?,
        url: String?,
        isReload: Boolean,
    ) {
        onEvent(
            WebViewEvent.NavigationStateChanged(
                currentUrl = url,
                canGoBack = view?.canGoBack() == true,
            ),
        )
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        onEvent(
            WebViewEvent.PageLoadFinished(
                currentUrl = url,
                canGoBack = view?.canGoBack() == true,
            ),
        )
    }
}
