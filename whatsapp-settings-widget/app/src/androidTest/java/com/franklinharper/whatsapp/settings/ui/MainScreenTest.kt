package com.franklinharper.whatsapp.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.franklinharper.whatsapp.settings.MainUiState
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsUnrestrictedStatus() {
        composeRule.setContent {
            MainScreen(
                uiState = MainUiState(WhatsAppStatus.BackgroundUsageUnrestricted),
                onOpenSettingsClick = {},
            )
        }
        composeRule.onNodeWithText("Background usage: Unrestricted").assertIsDisplayed()
    }

    @Test
    fun showsRestrictedOrOptimizedStatus() {
        composeRule.setContent {
            MainScreen(
                uiState = MainUiState(WhatsAppStatus.BackgroundUsageRestrictedOrOptimized),
                onOpenSettingsClick = {},
            )
        }
        composeRule.onNodeWithText("Background usage: Restricted or optimized").assertIsDisplayed()
    }

    @Test
    fun showsNotInstalledAndButtonDisabled() {
        composeRule.setContent {
            MainScreen(
                uiState = MainUiState(WhatsAppStatus.NotInstalled),
                onOpenSettingsClick = {},
            )
        }
        composeRule.onNodeWithText("WhatsApp not installed").assertIsDisplayed()
        composeRule.onNodeWithText("Open WhatsApp Battery Settings").assertIsNotEnabled()
    }

    @Test
    fun buttonClickInvokesCallback() {
        var clicked = false
        composeRule.setContent {
            MainScreen(
                uiState = MainUiState(WhatsAppStatus.BackgroundUsageUnrestricted),
                onOpenSettingsClick = { clicked = true },
            )
        }
        composeRule.onNodeWithText("Open WhatsApp Battery Settings").performClick()
        assertTrue(clicked)
    }

    @Test
    fun buttonEnabledWhenUnrestricted() {
        composeRule.setContent {
            MainScreen(
                uiState = MainUiState(WhatsAppStatus.BackgroundUsageUnrestricted),
                onOpenSettingsClick = {},
            )
        }
        composeRule.onNodeWithText("Open WhatsApp Battery Settings").assertIsEnabled()
    }
}
