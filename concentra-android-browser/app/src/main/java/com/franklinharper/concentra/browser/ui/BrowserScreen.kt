package com.franklinharper.concentra.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.franklinharper.concentra.browser.model.BrowserUiState
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.web.PasskeyBridge
import com.franklinharper.concentra.browser.web.WebViewCommand
import com.franklinharper.concentra.browser.web.WebViewEvent
import com.franklinharper.concentra.browser.web.WebViewHost
import com.franklinharper.concentra.browser.web.BrowserDownloadHandler
import kotlin.math.abs

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
    val density = LocalDensity.current
    val hotspotSizePx = with(density) { 64.dp.toPx() }
    val bottomInsetPx =
        WindowInsets.navigationBars.getBottom(density) +
            WindowInsets.ime.getBottom(density)

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
                .then(
                    if (!uiState.isChromeVisible) {
                        Modifier.pointerInput(hotspotSizePx, bottomInsetPx) {
                            detectHotspotSwipeUp(
                                hotspotSizePx = hotspotSizePx,
                                bottomInsetPx = bottomInsetPx,
                                onSwipeUp = onHotspotSwipeUp,
                            )
                        }
                    } else {
                        Modifier
                    },
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
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding(),
            )
        }
    }
}

private suspend fun PointerInputScope.detectHotspotSwipeUp(
    hotspotSizePx: Float,
    bottomInsetPx: Int,
    onSwipeUp: () -> Unit,
) {
    val hotspotLeft = size.width - hotspotSizePx
    val hotspotTop = size.height - bottomInsetPx - hotspotSizePx

    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.position.x < hotspotLeft || down.position.y < hotspotTop) continue

            var totalDragY = 0f
            var totalDragX = 0f
            val pointer = down.id

            while (true) {
                val moveEvent = awaitPointerEvent(PointerEventPass.Initial)
                val change =
                    moveEvent.changes.firstOrNull { it.id == pointer } ?: break
                if (!change.pressed) break

                val delta = change.positionChange()
                totalDragX += delta.x
                totalDragY += delta.y

                if (totalDragY <= -48f && abs(totalDragY) > abs(totalDragX)) {
                    change.consume()
                    onSwipeUp()
                    // Consume all remaining events until UP to prevent WebView tap
                    while (true) {
                        val remaining = awaitPointerEvent(PointerEventPass.Initial)
                        val remainingChange =
                            remaining.changes.firstOrNull { it.id == pointer } ?: break
                        remainingChange.consume()
                        if (!remainingChange.pressed) break
                    }
                    break
                }
            }
        }
    }
}

object BrowserScreenTags {
    const val ChromeSheet = "browser_chrome_sheet"
    const val UrlField = "browser_url_field"
    const val HotspotOverlay = "browser_hotspot_overlay"
}
