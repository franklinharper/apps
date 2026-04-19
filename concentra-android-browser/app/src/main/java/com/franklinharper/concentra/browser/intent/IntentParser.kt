package com.franklinharper.concentra.browser.intent

import android.content.Intent
import com.franklinharper.concentra.browser.model.LaunchRequest

class IntentParser {
    private val urlPattern = Regex("https?://[^\\s]+")
    private val trailingPunctuation = charArrayOf(')', '.', ',', ']', '}', ';', ':', '!', '?')

    fun parse(intent: Intent?): LaunchRequest {
        if (intent == null) {
            return LaunchRequest.Empty
        }

        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString?.let { LaunchRequest.OpenUrl(it) }
                ?: LaunchRequest.Empty
            Intent.ACTION_SEND -> parseSharedText(intent)
            else -> LaunchRequest.Empty
        }
    }

    private fun parseSharedText(intent: Intent): LaunchRequest {
        if (intent.type != "text/plain") {
            return LaunchRequest.Empty
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return LaunchRequest.Empty
        val url = urlPattern.find(sharedText)?.value?.trimEnd(*trailingPunctuation)
            ?: return LaunchRequest.Empty

        return LaunchRequest.OpenUrl(url)
    }
}
