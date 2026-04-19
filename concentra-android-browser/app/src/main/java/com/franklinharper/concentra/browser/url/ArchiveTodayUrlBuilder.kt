package com.franklinharper.concentra.browser.url

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ArchiveTodayUrlBuilder {
    fun build(currentUrl: String): String {
        val encoded = URLEncoder.encode(currentUrl, StandardCharsets.UTF_8)
        return "https://archive.ph/$encoded"
    }
}
