package com.franklinharper.concentra.browser.web

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

class PasskeyBridge(
    private val context: Context,
    private val webView: WebView,
) {
    private val tag = "PasskeyBridge"
    private val passkeyManager = PasskeyManager()
    private var pendingCreateRequestId: String? = null
    private var pendingGetRequestId: String? = null

    var launchIntentCallback: ((android.app.PendingIntent, Int) -> Unit)? = null

    @JavascriptInterface
    fun passkeyCreate(requestId: String, json: String, origin: String) {
        Log.d(tag, "passkeyCreate: requestId=$requestId origin=$origin json=${json.take(200)}")
        val result = passkeyManager.create(context, json, origin)
        Log.d(tag, "passkeyCreate result: $result")
        when (result) {
            is PasskeyResult.LaunchIntent -> {
                pendingCreateRequestId = requestId
                launchIntentCallback?.invoke(result.pendingIntent, result.requestCode)
            }
            else -> sendResult(requestId, result)
        }
    }

    @JavascriptInterface
    fun passkeyGet(requestId: String, json: String, origin: String) {
        Log.d(tag, "passkeyGet: requestId=$requestId origin=$origin json=${json.take(200)}")
        val result = passkeyManager.get(context, json, origin)
        Log.d(tag, "passkeyGet result: $result")
        when (result) {
            is PasskeyResult.LaunchIntent -> {
                pendingGetRequestId = requestId
                launchIntentCallback?.invoke(result.pendingIntent, result.requestCode)
            }
            else -> sendResult(requestId, result)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        Log.d(tag, "onActivityResult: requestCode=$requestCode resultCode=$resultCode")
        if (data == null) {
            val requestId = when (requestCode) {
                PasskeyManager.REGISTER_REQUEST_CODE -> pendingCreateRequestId
                PasskeyManager.SIGN_REQUEST_CODE -> pendingGetRequestId
                else -> null
            }
            if (requestId != null) sendResult(requestId, PasskeyResult.Cancelled)
            return
        }

        val result = PasskeyManager.handleResult(data)
        val requestId = when (requestCode) {
            PasskeyManager.REGISTER_REQUEST_CODE -> pendingCreateRequestId.also { pendingCreateRequestId = null }
            PasskeyManager.SIGN_REQUEST_CODE -> pendingGetRequestId.also { pendingGetRequestId = null }
            else -> null
        }
        if (requestId != null) sendResult(requestId, result)
    }

    private fun sendResult(requestId: String, result: PasskeyResult) {
        Log.d(tag, "sendResult: requestId=$requestId result=$result")
        when (result) {
            is PasskeyResult.Success -> resolve(requestId, result.responseJson)
            is PasskeyResult.Cancelled -> reject(requestId, "cancelled", "User cancelled")
            is PasskeyResult.NotSupported -> reject(requestId, "not_supported", "Passkeys require API 23+")
            is PasskeyResult.Failure -> reject(requestId, result.errorType, result.message)
            is PasskeyResult.LaunchIntent -> {}
        }
    }

    private fun resolve(requestId: String, responseJson: String) {
        val escaped = responseJson
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
        eval("window.__passkeyResolve('$requestId', '$escaped')")
    }

    private fun reject(requestId: String, errorType: String, message: String) {
        val escaped = message.replace("'", "\\'")
        eval("window.__passkeyReject('$requestId', '$errorType', '$escaped')")
    }

    private fun eval(js: String) {
        webView.post { webView.evaluateJavascript(js, null) }
    }
}
