package com.franklinharper.concentra.browser.settings

import android.content.SharedPreferences

class PreferencesSettingsRepository(
    private val sharedPreferences: SharedPreferences,
) : SettingsRepository {
    override fun load(): BrowserSettings =
        BrowserSettings(
            thirdPartyCookiesEnabled = sharedPreferences.getBoolean(KEY_THIRD_PARTY_COOKIES_ENABLED, false),
        )

    override fun saveThirdPartyCookiesEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_THIRD_PARTY_COOKIES_ENABLED, enabled)
            .apply()
    }

    private companion object {
        const val KEY_THIRD_PARTY_COOKIES_ENABLED = "third_party_cookies_enabled"
    }
}
