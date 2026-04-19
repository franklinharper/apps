package com.franklinharper.concentra.browser.intent

import android.content.Intent
import com.franklinharper.concentra.browser.model.LaunchRequest

class IntentParser {
    private val urlPattern = Regex("https?://[^\\s]+")
    private val alwaysTrimmedPunctuation = charArrayOf('.', ',', ';', ':', '!', '?')

    fun parse(intent: Intent?): LaunchRequest {
        if (intent == null) {
            return LaunchRequest.Empty
        }

        return when (intent.action) {
            Intent.ACTION_VIEW -> parseViewIntent(intent)
            Intent.ACTION_SEND -> parseSharedText(intent)
            else -> LaunchRequest.Empty
        }
    }

    private fun parseViewIntent(intent: Intent): LaunchRequest {
        val scheme = intent.scheme?.lowercase()
        val url = intent.dataString

        return if (url != null && (scheme == "http" || scheme == "https")) {
            LaunchRequest.OpenUrl(url)
        } else {
            LaunchRequest.Empty
        }
    }

    private fun parseSharedText(intent: Intent): LaunchRequest {
        if (intent.type != "text/plain") {
            return LaunchRequest.Empty
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return LaunchRequest.Empty
        val url = urlPattern.find(sharedText)?.value?.let(::trimTrailingPunctuation)
            ?: return LaunchRequest.Empty

        return LaunchRequest.OpenUrl(url)
    }

    private fun trimTrailingPunctuation(value: String): String {
        var trimmed = value.trimEnd(*alwaysTrimmedPunctuation)

        trimmed = trimUnbalancedClosing(trimmed, ')', '(')
        trimmed = trimUnbalancedClosing(trimmed, ']', '[')
        trimmed = trimUnbalancedClosing(trimmed, '}', '{')

        return trimmed
    }

    private fun trimUnbalancedClosing(value: String, closing: Char, opening: Char): String {
        var trimmed = value

        while (trimmed.endsWith(closing) && count(trimmed, closing) > count(trimmed, opening)) {
            trimmed = trimmed.dropLast(1)
        }

        return trimmed
    }

    private fun count(value: String, char: Char): Int = value.count { it == char }
}
