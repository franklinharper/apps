package com.franklinharper.wordlecoach.androidApp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.franklinharper.wordlecoach.App
import com.franklinharper.wordlecoach.domain.PuzzleResult

class AppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val puzzle: PuzzleResult? = parseSharedIntent(intent)
        setContent {
            App(
                puzzle = puzzle,
                onThemeChanged = { ThemeChanged(it) },
            )
        }
    }

    /**
     * Extracts a [PuzzleResult] from a share intent when the app is opened as a share target.
     *
     * The intent carries the screenshot URI in [Intent.EXTRA_STREAM].
     * Parsing the image (OCR of the board tiles) is a TODO — a real implementation would use
     * ML Kit's text recogniser or a custom vision pipeline to read the letter and colour of
     * each tile and build a [PuzzleResult] from them.
     *
     * Returns `null` if the intent is not a share intent or if parsing is not yet implemented.
     */
    private fun parseSharedIntent(intent: Intent?): PuzzleResult? {
        if (intent?.action != Intent.ACTION_SEND) return null
        @Suppress("DEPRECATION")
        val imageUri: Uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: return null
        // TODO: run OCR on imageUri to extract tile letters and colours, then build PuzzleResult
        android.util.Log.d("WordleCoach", "Received shared image: $imageUri — OCR not yet implemented")
        return null
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
