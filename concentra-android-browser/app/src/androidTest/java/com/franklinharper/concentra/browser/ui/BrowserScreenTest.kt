package com.franklinharper.concentra.browser.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.franklinharper.concentra.browser.model.BrowserUiState
import com.franklinharper.concentra.browser.web.BrowserDownloadHandler
import com.franklinharper.concentra.browser.BrowserActivity
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class BrowserScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<BrowserActivity>()

    @Test
    fun empty_launch_shows_chrome() {
        composeRule.onNodeWithTag(BrowserScreenTags.ChromeSheet).assertIsDisplayed()
        composeRule.onNodeWithTag(BrowserScreenTags.UrlField).assertIsDisplayed()
        composeRule.onAllNodesWithTag(BrowserScreenTags.HotspotOverlay).assertCountEquals(0)
    }

    @Test
    fun archive_share_find_are_disabled_without_url() {
        composeRule.onNodeWithText("Archive").assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText("Share").assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText("Find").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun raw_tap_on_google_button_invokes_callback() {
        var googleClicked = false

        composeRule.setContent {
            val context = LocalContext.current
            val urlInput = remember { mutableStateOf("") }
            BrowserScreen(
                uiState = BrowserUiState(isChromeVisible = true),
                urlInput = urlInput.value,
                downloadHandler = BrowserDownloadHandler(context),
                pendingWebCommand = null,
                pendingWebEffect = null,
                onWebCommandConsumed = {},
                onWebEffectConsumed = {},
                onWebViewEvent = {},
                onUrlInputChange = { urlInput.value = it },
                onUrlSubmit = {},
                onGoogleClick = { googleClicked = true },
                onArchiveClick = {},
                onShareClick = {},
                onFindClick = {},
                onSettingsClick = {},
                onExitClick = {},
                onHotspotSwipeUp = {},
                onChromeScrimTap = {},
            )
        }

        val googleCenter = composeRule.onNodeWithText("Google").centerInRoot()
        composeRule.onRoot().performTouchInput {
            click(googleCenter)
        }

        composeRule.runOnIdle {
            assertTrue(googleClicked)
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.centerInRoot(): Offset =
    fetchSemanticsNode().let { node ->
        Offset(
            x = node.boundsInRoot.center.x,
            y = node.boundsInRoot.center.y,
        )
    }
