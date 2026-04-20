package com.franklinharper.concentra.browser.web

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.CookieManager
import android.webkit.WebView
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.settings.BrowserSettings

@Composable
fun WebViewHost(
    settings: BrowserSettings,
    downloadHandler: BrowserDownloadHandler,
    command: WebViewCommand?,
    effect: BrowserViewModel.Effect?,
    onCommandConsumed: () -> Unit,
    onEffectConsumed: () -> Unit,
    onEvent: (WebViewEvent) -> Unit,
    onBridgeCreated: (PasskeyBridge) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configurator = remember { WebViewConfigurator() }
    val currentOnEvent = rememberUpdatedState(onEvent)
    val currentDownloadHandler = rememberUpdatedState(downloadHandler)

    val polyfillJs = remember {
        context.assets.open("passkey-polyfill.js").bufferedReader().use { it.readText() }
    }

    val webView =
        remember(context) {
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                webChromeClient = BrowserWebChromeClient()
                webViewClient =
                    BrowserWebViewClient(
                        onEvent = { event -> currentOnEvent.value(event) },
                        onPageStarted = { view ->
                            android.util.Log.d("WebViewHost", "onPageStarted injecting polyfill, url=")
                            view?.evaluateJavascript(polyfillJs, null)
                        },
                    )
            }
        }

    val bridge = remember(webView) {
        PasskeyBridge(webView, context as? Activity).also { b ->
            webView.addJavascriptInterface(b, "Android")
            onBridgeCreated(b)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }

    LaunchedEffect(webView, command) {
        when (val currentCommand = command) {
            is WebViewCommand.LoadUrl -> {
                webView.loadUrl(currentCommand.url)
                onCommandConsumed()
            }
            null -> Unit
        }
    }

    LaunchedEffect(webView, effect) {
        when (effect) {
            BrowserViewModel.Effect.GoBack -> {
                if (webView.canGoBack()) {
                    webView.goBack()
                }
                onEffectConsumed()
            }
            BrowserViewModel.Effect.OpenFindInPage -> {
                @Suppress("DEPRECATION")
                webView.showFindDialog(null, true)
                onEffectConsumed()
            }
            null -> Unit
            else -> Unit
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { currentWebView ->
            currentWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                currentDownloadHandler.value.enqueue(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                )
            }
            settings.applyTo(
                webView = currentWebView,
                configurator = configurator,
            )
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
