package com.franklinharper.concentra.browser.model

sealed interface BrowserAction {
    data class SubmitUrl(val rawInput: String) : BrowserAction

    data object GoogleClicked : BrowserAction

    data object ArchiveTodayClicked : BrowserAction

    data object ShareLinkClicked : BrowserAction

    data object FindInPageClicked : BrowserAction

    data object OpenSettingsClicked : BrowserAction

    data object ExitClicked : BrowserAction

    data object BackPressed : BrowserAction

    data object ShowChrome : BrowserAction

    data object HideChrome : BrowserAction
}
