package com.franklinharper.concentra.browser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.franklinharper.concentra.browser.BrowserAppContainer
import com.franklinharper.concentra.browser.BrowserViewModel

@Composable
fun BrowserRoute(container: BrowserAppContainer) {
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

    LaunchedEffect(uiState.pendingUrlInput) {
        urlInput = uiState.pendingUrlInput
    }

    BrowserScreen(
        uiState = uiState,
        urlInput = urlInput,
        onUrlInputChange = { urlInput = it },
        onUrlSubmit = {},
        onGoogleClick = {},
        onArchiveClick = {},
        onShareClick = {},
        onFindClick = {},
        onSettingsClick = {},
        onExitClick = {},
    )
}
