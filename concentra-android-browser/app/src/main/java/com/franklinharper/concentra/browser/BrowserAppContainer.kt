package com.franklinharper.concentra.browser

import android.content.Context
import com.franklinharper.concentra.browser.intent.IntentParser
import com.franklinharper.concentra.browser.model.LaunchRequest
import com.franklinharper.concentra.browser.settings.PreferencesSettingsRepository
import com.franklinharper.concentra.browser.settings.SettingsRepository

class BrowserAppContainer(
    activity: BrowserActivity,
    intentParser: IntentParser = IntentParser(),
) {
    private val appContext = activity.applicationContext

    val settingsRepository: SettingsRepository =
        PreferencesSettingsRepository(
            sharedPreferences =
                appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        )

    val launchRequest: LaunchRequest = intentParser.parse(activity.intent)

    private companion object {
        const val PREFERENCES_NAME = "browser_settings"
    }
}
