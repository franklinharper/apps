package com.franklinharper.concentra.browser

import com.franklinharper.concentra.browser.model.BrowserAction
import com.franklinharper.concentra.browser.model.LaunchRequest
import com.franklinharper.concentra.browser.settings.BrowserSettings
import com.franklinharper.concentra.browser.settings.SettingsRepository
import com.franklinharper.concentra.browser.web.WebViewCommand
import com.franklinharper.concentra.browser.web.WebViewEvent
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
        assertEquals(
            WebViewCommand.LoadUrl("https://example.com"),
            viewModel.consumePendingWebCommand(),
        )
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

        assertEquals(
            WebViewCommand.LoadUrl("https://example.com"),
            viewModel.consumePendingWebCommand(),
        )
        assertFalse(viewModel.uiState.value.isChromeVisible)
    }

    @Test
    fun `blank submit url does not emit load command`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onAction(BrowserAction.SubmitUrl("   "))

        assertNull(viewModel.consumePendingWebCommand())
        assertTrue(viewModel.uiState.value.isChromeVisible)
    }

    @Test
    fun `google action emits google load command`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onAction(BrowserAction.GoogleClicked)

        assertEquals(
            WebViewCommand.LoadUrl("https://www.google.com"),
            viewModel.consumePendingWebCommand(),
        )
        assertNull(viewModel.consumePendingWebCommand())
    }

    @Test
    fun `archive action emits archive url when current url exists`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.onWebViewEvent(
            WebViewEvent.NavigationStateChanged(
                currentUrl = "https://example.com",
                canGoBack = false,
            ),
        )

        viewModel.onAction(BrowserAction.ArchiveTodayClicked)

        assertEquals(
            WebViewCommand.LoadUrl("https://archive.ph/https%3A%2F%2Fexample.com"),
            viewModel.consumePendingWebCommand(),
        )
        assertNull(viewModel.consumePendingWebCommand())
    }

    @Test
    fun `share action emits share effect when current url exists`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.onWebViewEvent(
            WebViewEvent.NavigationStateChanged(
                currentUrl = "https://example.com",
                canGoBack = false,
            ),
        )

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
        viewModel.onWebViewEvent(
            WebViewEvent.NavigationStateChanged(
                currentUrl = "https://example.com",
                canGoBack = false,
            ),
        )

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
        viewModel.onWebViewEvent(
            WebViewEvent.NavigationStateChanged(
                currentUrl = "https://example.com",
                canGoBack = true,
            ),
        )
        viewModel.onAction(BrowserAction.ShowChrome)

        viewModel.onAction(BrowserAction.BackPressed)

        assertFalse(viewModel.uiState.value.isChromeVisible)
        assertNull(viewModel.consumePendingEffect())

        viewModel.onAction(BrowserAction.BackPressed)

        assertEquals(BrowserViewModel.Effect.GoBack, viewModel.consumePendingEffect())
        assertNull(viewModel.consumePendingEffect())
    }

    @Test
    fun `back exits immediately on empty launch`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onAction(BrowserAction.BackPressed)

        assertEquals(BrowserViewModel.Effect.Exit, viewModel.consumePendingEffect())
        assertTrue(viewModel.uiState.value.isChromeVisible)
    }

    @Test
    fun `hide chrome action closes chrome`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)
        viewModel.onAction(BrowserAction.ShowChrome)

        viewModel.onAction(BrowserAction.HideChrome)

        assertFalse(viewModel.uiState.value.isChromeVisible)
    }

    @Test
    fun `navigation event updates current url and enables current page actions`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onWebViewEvent(
            WebViewEvent.NavigationStateChanged(
                currentUrl = "https://example.com",
                canGoBack = true,
            ),
        )

        assertEquals("https://example.com", viewModel.uiState.value.currentUrl)
        assertTrue(viewModel.uiState.value.canGoBack)
        assertTrue(viewModel.uiState.value.isArchiveTodayEnabled)
        assertTrue(viewModel.uiState.value.isShareEnabled)
        assertTrue(viewModel.uiState.value.isFindInPageEnabled)
    }

    @Test
    fun `page loading events update loading state`() {
        val viewModel = buildViewModel(LaunchRequest.Empty)

        viewModel.onWebViewEvent(WebViewEvent.PageLoadStarted(url = "https://example.com"))

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals("https://example.com", viewModel.uiState.value.currentUrl)

        viewModel.onWebViewEvent(
            WebViewEvent.PageLoadFinished(
                currentUrl = "https://example.com",
                canGoBack = false,
            ),
        )

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("https://example.com", viewModel.uiState.value.currentUrl)
    }

    @Test
    fun `reload settings refreshes browser settings from repository`() {
        val settingsRepository = FakeSettingsRepository()
        val viewModel =
            BrowserViewModel(
                launchRequest = LaunchRequest.Empty,
                settingsRepository = settingsRepository,
            )

        settingsRepository.settings = BrowserSettings(thirdPartyCookiesEnabled = true)

        viewModel.reloadSettings()

        assertTrue(viewModel.uiState.value.settings.thirdPartyCookiesEnabled)
    }

    private fun buildViewModel(launchRequest: LaunchRequest): BrowserViewModel =
        BrowserViewModel(
            launchRequest = launchRequest,
            settingsRepository = FakeSettingsRepository(),
        )
}

private class FakeSettingsRepository(
    var settings: BrowserSettings = BrowserSettings(),
) : SettingsRepository {
    override fun load(): BrowserSettings = settings

    override fun saveThirdPartyCookiesEnabled(enabled: Boolean) = Unit
}
