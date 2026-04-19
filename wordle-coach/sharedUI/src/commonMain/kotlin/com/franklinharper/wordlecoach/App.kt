package com.franklinharper.wordlecoach

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import com.franklinharper.wordlecoach.domain.CoachingState
import com.franklinharper.wordlecoach.domain.PuzzleResult
import com.franklinharper.wordlecoach.theme.AppTheme

/**
 * Root composable.
 *
 * @param puzzle       Parsed puzzle received from the share-target entry point.
 *                     `null` means the app was launched directly (not via a share),
 *                     so only the pre-game step 1 coaching is shown.
 * @param imageBitmap  The raw shared screenshot as a bitmap, for debug display on step 1.
 *                     `null` when the app was launched directly.
 */
@Preview
@Composable
fun App(
    puzzle: PuzzleResult? = null,
    imageBitmap: ImageBitmap? = null,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) = AppTheme(onThemeChanged) {
    val initialState = remember(puzzle) {
        if (puzzle != null) CoachingState.fromPuzzle(puzzle)
        else CoachingState.initial()
    }
    var state by remember(initialState) { mutableStateOf(initialState) }

    CoachingScreen(
        state = state,
        imageBitmap = imageBitmap,
        onBack    = { state = state.goBack() },
        onForward = { state = state.goForward() },
    )
}
