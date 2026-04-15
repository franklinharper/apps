package com.franklinharper.wordlecoach

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.franklinharper.wordlecoach.domain.CoachingState
import com.franklinharper.wordlecoach.theme.AppTheme

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}
) = AppTheme(onThemeChanged) {
    var state by remember { mutableStateOf(CoachingState.initial()) }

    CoachingScreen(
        state = state,
        onBack = { state = state.goBack() },
        onForward = { state = state.goForward() },
    )
}
