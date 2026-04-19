package com.franklinharper.concentra.browser.url

class ArchiveTodayUrlBuilder {
    fun build(currentUrl: String): String = "https://archive.ph/$currentUrl"
}
