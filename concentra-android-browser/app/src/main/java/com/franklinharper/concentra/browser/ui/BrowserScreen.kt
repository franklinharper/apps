package com.franklinharper.concentra.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.franklinharper.concentra.browser.model.BrowserUiState
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.web.PasskeyBridge
import com.franklinharper.concentra.browser.web.WebViewCommand
import com.franklinharper.concentra.browser.web.WebViewEvent
import com.franklinharper.concentra.browser.web.WebViewHost
import com.franklinharper.concentra.browser.web.BrowserDownloadHandler

@Composable
fun BrowserScreen(
    uiState: BrowserUiState,
    urlInput: String,
    downloadHandler: BrowserDownloadHandler,
    pendingWebCommand: WebViewCommand?,
    pendingWebEffect: BrowserViewModel.Effect?,
    onWebCommandConsumed: () -> Unit,
    onWebEffectConsumed: () -> Unit,
    onWebViewEvent: (WebViewEvent) -> Unit,
    onUrlInputChange: (String) -> Unit,
    onUrlSubmit: () -> Unit,
    onGoogleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onShareClick: () -> Unit,
    onFindClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    onHotspotSwipeUp: () -> Unit,
    onChromeScrimTap: () -> Unit,
    onBridgeCreated: (PasskeyBridge) -> Unit = {},
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFE9E2D6), Color(0xFFF7F2EA)),
                        ),
                )
                .semantics {
                    testTagsAsResourceId = true
                },
    ) {
        WebViewHost(
            settings = uiState.settings,
            downloadHandler = downloadHandler,
            command = pendingWebCommand,
            effect = pendingWebEffect,
            onCommandConsumed = onWebCommandConsumed,
            onEffectConsumed = onWebEffectConsumed,
            onEvent = onWebViewEvent,
            onBridgeCreated = onBridgeCreated,
            modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding(),
        )

        if (uiState.isChromeVisible) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.08f))
                            .clickable(onClick = onChromeScrimTap),
                )

                BrowserChromeSheet(
                    uiState = uiState,
                    urlInput = urlInput,
                    onUrlInputChange = onUrlInputChange,
                    onUrlSubmit = onUrlSubmit,
                    onGoogleClick = onGoogleClick,
                    onArchiveClick = onArchiveClick,
                    onShareClick = onShareClick,
                    onFindClick = onFindClick,
                    onSettingsClick = onSettingsClick,
                    onExitClick = onExitClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                )
            }
        }

        if (!uiState.isChromeVisible) {
            HotspotOverlay(
                onSwipeUp = onHotspotSwipeUp,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .imePadding(),
            )
        }
    }
}

object BrowserScreenTags {
    const val ChromeSheet = "browser_chrome_sheet"
    const val UrlField = "browser_url_field"
    const val HotspotOverlay = "browser_hotspot_overlay"
}
