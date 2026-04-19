package com.franklinharper.concentra.browser.settings

data class BrowserSettings(
    val thirdPartyCookiesEnabled: Boolean = false,
)

interface SettingsRepository {
    fun load(): BrowserSettings
    fun saveThirdPartyCookiesEnabled(enabled: Boolean)
}
