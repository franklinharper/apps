package com.franklinharper.concentra.browser

import com.franklinharper.concentra.browser.model.LaunchRequest
import com.franklinharper.concentra.browser.settings.BrowserSettings
import com.franklinharper.concentra.browser.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserViewModelTest {
    @Test
    fun `empty launch shows chrome immediately`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        assertTrue(viewModel.uiState.value.isChromeVisible)
        assertNull(viewModel.uiState.value.currentUrl)
    }

    @Test
    fun `url launch hides chrome and emits load command`() {
        val viewModel = buildViewModel(LaunchRequest.OpenUrl("https://example.com"))

        assertFalse(viewModel.uiState.value.isChromeVisible)
        assertEquals("https://example.com", viewModel.pendingWebCommand())
    }

    @Test
    fun `archive action is disabled with no current url`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        assertFalse(viewModel.uiState.value.isArchiveTodayEnabled)
    }

    private fun buildViewModel(launchRequest: LaunchRequest): BrowserViewModel =
        BrowserViewModel(
            launchRequest = launchRequest,
            settingsRepository = FakeSettingsRepository(),
        )
}

private class FakeSettingsRepository(
    private val settings: BrowserSettings = BrowserSettings(),
) : SettingsRepository {
    override fun load(): BrowserSettings = settings

    override fun saveThirdPartyCookiesEnabled(enabled: Boolean) = Unit
}
