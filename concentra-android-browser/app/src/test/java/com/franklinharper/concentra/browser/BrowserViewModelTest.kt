package com.franklinharper.concentra.browser

import com.franklinharper.concentra.browser.model.BrowserAction
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
        assertNull(viewModel.uiState.value.currentUrl)
        assertEquals(BrowserSettings(), viewModel.uiState.value.settings)
        assertEquals("https://example.com", viewModel.consumePendingWebCommand())
        assertNull(viewModel.consumePendingWebCommand())
    }

    @Test
    fun `archive action is disabled with no current url`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        assertFalse(viewModel.uiState.value.isArchiveTodayEnabled)
    }

    @Test
    fun `submit url action normalizes and emits load command`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onAction(BrowserAction.SubmitUrl("example.com"))

        assertEquals("https://example.com", viewModel.consumePendingWebCommand())
        assertFalse(viewModel.uiState.value.isChromeVisible)
    }

    @Test
    fun `google action emits google load command`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onAction(BrowserAction.GoogleClicked)

        assertEquals("https://www.google.com", viewModel.consumePendingWebCommand())
        assertNull(viewModel.consumePendingWebCommand())
    }

    @Test
    fun `archive action emits archive url when current url exists`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.updatePageState(currentUrl = "https://example.com")

        viewModel.onAction(BrowserAction.ArchiveTodayClicked)

        assertEquals(
            "https://archive.ph/https%3A%2F%2Fexample.com",
            viewModel.consumePendingWebCommand(),
        )
        assertNull(viewModel.consumePendingWebCommand())
    }

    @Test
    fun `share action emits share effect when current url exists`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.updatePageState(currentUrl = "https://example.com")

        viewModel.onAction(BrowserAction.ShareLinkClicked)

        assertEquals(
            BrowserViewModel.Effect.ShareUrl("https://example.com"),
            viewModel.consumePendingEffect(),
        )
        assertNull(viewModel.consumePendingEffect())
    }

    @Test
    fun `settings action emits open settings effect`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onAction(BrowserAction.OpenSettingsClicked)

        assertEquals(BrowserViewModel.Effect.OpenSettings, viewModel.consumePendingEffect())
        assertNull(viewModel.consumePendingEffect())
    }

    @Test
    fun `find in page action emits effect when current url exists`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.updatePageState(currentUrl = "https://example.com")

        viewModel.onAction(BrowserAction.FindInPageClicked)

        assertEquals(BrowserViewModel.Effect.OpenFindInPage, viewModel.consumePendingEffect())
        assertNull(viewModel.consumePendingEffect())
    }

    @Test
    fun `exit action emits exit effect`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onAction(BrowserAction.ExitClicked)

        assertEquals(BrowserViewModel.Effect.Exit, viewModel.consumePendingEffect())
        assertNull(viewModel.consumePendingEffect())
    }

    @Test
    fun `back closes chrome before navigating web history`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.updatePageState(currentUrl = "https://example.com", canGoBack = true)
        viewModel.onAction(BrowserAction.ShowChrome)

        viewModel.onAction(BrowserAction.BackPressed)

        assertFalse(viewModel.uiState.value.isChromeVisible)
        assertNull(viewModel.consumePendingEffect())

        viewModel.onAction(BrowserAction.BackPressed)

        assertEquals(BrowserViewModel.Effect.GoBack, viewModel.consumePendingEffect())
        assertNull(viewModel.consumePendingEffect())
    }

    @Test
    fun `hide chrome action closes chrome`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.onAction(BrowserAction.ShowChrome)

        viewModel.onAction(BrowserAction.HideChrome)

        assertFalse(viewModel.uiState.value.isChromeVisible)
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
