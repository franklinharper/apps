package com.franklinharper.concentra.browser.web

sealed class PasskeyResult {
    data class Success(val responseJson: String) : PasskeyResult()
    data class Failure(val errorType: String, val message: String) : PasskeyResult()
    object NotSupported : PasskeyResult()
    object Cancelled : PasskeyResult()
    data class LaunchIntent(val pendingIntent: android.app.PendingIntent, val requestCode: Int) : PasskeyResult()
}
