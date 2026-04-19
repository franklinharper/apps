package com.franklinharper.concentra.browser.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.franklinharper.concentra.browser.BrowserAppContainer
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.model.BrowserAction
import com.franklinharper.concentra.browser.web.WebViewCommand

@Composable
fun BrowserRoute(container: BrowserAppContainer) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: BrowserViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        BrowserViewModel(
                            launchRequest = container.launchRequest,
                            settingsRepository = container.settingsRepository,
                        )
                    }
                },
        )
    val uiState by viewModel.uiState.collectAsState()
    var urlInput by rememberSaveable { mutableStateOf(uiState.pendingUrlInput) }
    var pendingWebCommand by remember { mutableStateOf<WebViewCommand?>(null) }
    var pendingWebEffect by remember { mutableStateOf<BrowserViewModel.Effect?>(null) }

    LaunchedEffect(uiState.pendingUrlInput) {
        urlInput = uiState.pendingUrlInput
    }

    LaunchedEffect(uiState) {
        viewModel.consumePendingWebCommand()?.let { pendingWebCommand = it }
        viewModel.consumePendingEffect()?.let { effect ->
            when (effect) {
                is BrowserViewModel.Effect.ShareUrl -> {
                    val shareIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, effect.url)
                        }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
                BrowserViewModel.Effect.Exit -> activity?.finish()
                BrowserViewModel.Effect.GoBack,
                BrowserViewModel.Effect.OpenFindInPage,
                BrowserViewModel.Effect.OpenSettings,
                    -> pendingWebEffect = effect
            }
        }
    }

    BrowserScreen(
        uiState = uiState,
        urlInput = urlInput,
        pendingWebCommand = pendingWebCommand,
        pendingWebEffect = pendingWebEffect,
        onWebCommandConsumed = { pendingWebCommand = null },
        onWebEffectConsumed = { pendingWebEffect = null },
        onWebViewEvent = viewModel::onWebViewEvent,
        onUrlInputChange = { urlInput = it },
        onUrlSubmit = { viewModel.onAction(BrowserAction.SubmitUrl(urlInput)) },
        onGoogleClick = { viewModel.onAction(BrowserAction.GoogleClicked) },
        onArchiveClick = { viewModel.onAction(BrowserAction.ArchiveTodayClicked) },
        onShareClick = { viewModel.onAction(BrowserAction.ShareLinkClicked) },
        onFindClick = { viewModel.onAction(BrowserAction.FindInPageClicked) },
        onSettingsClick = { viewModel.onAction(BrowserAction.OpenSettingsClicked) },
        onExitClick = { viewModel.onAction(BrowserAction.ExitClicked) },
        onHotspotSwipeUp = { viewModel.onAction(BrowserAction.ShowChrome) },
        onChromeScrimTap = { viewModel.onAction(BrowserAction.HideChrome) },
    )
}
