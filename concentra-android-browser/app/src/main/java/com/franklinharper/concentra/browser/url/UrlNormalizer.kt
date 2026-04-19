package com.franklinharper.concentra.browser.url

class UrlNormalizer {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
