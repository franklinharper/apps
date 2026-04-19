package com.franklinharper.concentra.browser.web

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PasskeyBridge(
    private val passkeyManager: PasskeyManager,
    private val webView: WebView,
    private val activity: Activity?,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun close() = scope.cancel()

    @JavascriptInterface
    fun passkeyCreate(requestId: String, json: String, origin: String) {
        scope.launch {
            val result = passkeyManager.create(activity, json, origin)
            sendResult(requestId, result)
        }
    }

    @JavascriptInterface
    fun passkeyGet(requestId: String, json: String, origin: String) {
        scope.launch {
            val result = passkeyManager.get(activity, json, origin)
            sendResult(requestId, result)
        }
    }

    private fun sendResult(requestId: String, result: PasskeyResult) {
        when (result) {
            is PasskeyResult.Success -> resolve(requestId, result.responseJson)
            is PasskeyResult.Cancelled -> reject(requestId, "cancelled", "User cancelled")
            is PasskeyResult.NotSupported -> reject(requestId, "not_supported", "Passkeys require API 26+")
            is PasskeyResult.Failure -> reject(requestId, result.errorType, result.message)
        }
    }

    private fun resolve(requestId: String, responseJson: String) {
        eval("window.__passkeyResolve('$requestId', $responseJson)")
    }

    private fun reject(requestId: String, errorType: String, message: String) {
        val escaped = message.replace("'", "\\'")
        eval("window.__passkeyReject('$requestId', '$errorType', '$escaped')")
    }

    private fun eval(js: String) {
        webView.post { webView.evaluateJavascript(js, null) }
    }
}
