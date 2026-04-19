package com.franklinharper.concentra.browser.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.franklinharper.concentra.browser.BrowserActivity
import org.junit.Rule
import org.junit.Test

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
}
