package com.franklinharper.concentra.browser.web

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.settings.BrowserSettings

@Composable
fun WebViewHost(
    settings: BrowserSettings,
    command: WebViewCommand?,
    effect: BrowserViewModel.Effect?,
    onCommandConsumed: () -> Unit,
    onEffectConsumed: () -> Unit,
    onEvent: (WebViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configurator = remember { WebViewConfigurator() }
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(webView) {
        onDispose {
            webView?.destroy()
        }
    }

    LaunchedEffect(webView, command) {
        val currentWebView = webView ?: return@LaunchedEffect
        when (val currentCommand = command) {
            is WebViewCommand.LoadUrl -> {
                currentWebView.loadUrl(currentCommand.url)
                onCommandConsumed()
            }
            null -> Unit
        }
    }

    LaunchedEffect(webView, effect) {
        val currentWebView = webView ?: return@LaunchedEffect
        when (effect) {
            BrowserViewModel.Effect.GoBack -> {
                if (currentWebView.canGoBack()) {
                    currentWebView.goBack()
                }
                onEffectConsumed()
            }
            BrowserViewModel.Effect.OpenFindInPage -> {
                currentWebView.showFindDialog("", true)
                onEffectConsumed()
            }
            null -> Unit
            else -> Unit
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                webChromeClient = BrowserWebChromeClient()
                webViewClient = BrowserWebViewClient(onEvent = onEvent)
                settings.applyTo(
                    webView = this,
                    configurator = configurator,
                )
                webView = this
            }
        },
        modifier = modifier,
        update = { currentWebView ->
            settings.applyTo(
                webView = currentWebView,
                configurator = configurator,
            )
            if (webView !== currentWebView) {
                webView = currentWebView
            }
        },
    )
}

private fun BrowserSettings.applyTo(
    webView: WebView,
    configurator: WebViewConfigurator,
) {
    configurator.configure(
        settings = this,
        sink =
            object : WebViewConfigurator.SettingsSink {
                override var javaScriptEnabled: Boolean
                    get() = webView.settings.javaScriptEnabled
                    set(value) {
                        webView.settings.javaScriptEnabled = value
                    }

                override var domStorageEnabled: Boolean
                    get() = webView.settings.domStorageEnabled
                    set(value) {
                        webView.settings.domStorageEnabled = value
                    }

                override var builtInZoomControlsEnabled: Boolean
                    get() = webView.settings.builtInZoomControls
                    set(value) {
                        webView.settings.builtInZoomControls = value
                    }

                override var displayZoomControlsEnabled: Boolean
                    get() = webView.settings.displayZoomControls
                    set(value) {
                        webView.settings.displayZoomControls = value
                    }

                override var supportZoomEnabled: Boolean
                    get() = webView.settings.supportZoom()
                    set(value) {
                        webView.settings.setSupportZoom(value)
                    }

                override var supportMultipleWindows: Boolean
                    get() = webView.settings.supportMultipleWindows()
                    set(value) {
                        webView.settings.setSupportMultipleWindows(value)
                    }

                override var thirdPartyCookiesEnabled: Boolean
                    get() = CookieManager.getInstance().acceptThirdPartyCookies(webView)
                    set(value) {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, value)
                    }
            },
    )
}
