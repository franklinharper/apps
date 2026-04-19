package com.franklinharper.concentra.browser.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun `view model loads initial cookie setting`() {
        val viewModel =
            SettingsViewModel(
                settingsRepository =
                    FakeSettingsRepository(
                        settings = BrowserSettings(thirdPartyCookiesEnabled = true),
                    ),
            )

        assertTrue(viewModel.uiState.value.thirdPartyCookiesEnabled)
    }

    @Test
    fun `toggling cookies updates state and repository`() {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(settingsRepository = repository)

        viewModel.onThirdPartyCookiesChanged(enabled = true)

        assertTrue(viewModel.uiState.value.thirdPartyCookiesEnabled)
        assertTrue(repository.settings.thirdPartyCookiesEnabled)

        viewModel.onThirdPartyCookiesChanged(enabled = false)

        assertFalse(viewModel.uiState.value.thirdPartyCookiesEnabled)
        assertFalse(repository.settings.thirdPartyCookiesEnabled)
    }
}

private class FakeSettingsRepository(
    var settings: BrowserSettings = BrowserSettings(),
) : SettingsRepository {
    override fun load(): BrowserSettings = settings

    override fun saveThirdPartyCookiesEnabled(enabled: Boolean) {
        settings = settings.copy(thirdPartyCookiesEnabled = enabled)
    }
}
