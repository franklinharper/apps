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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.franklinharper.wordlecoach.App
import com.franklinharper.wordlecoach.domain.PuzzleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Compose state — starts null (shows step-1 coaching immediately).
        // Updated asynchronously once Claude parses the shared screenshot.
        var puzzle by mutableStateOf<PuzzleResult?>(null)

        @Suppress("DEPRECATION")
        val imageUri: Uri? = if (intent?.action == Intent.ACTION_SEND)
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        else
            null

        if (imageUri != null) {
            lifecycleScope.launch {
                puzzle = withContext(Dispatchers.IO) {
                    WordleImageParser(this@AppActivity, BuildConfig.ANTHROPIC_API_KEY)
                        .parse(imageUri)
                }
            }
        }

        setContent {
            App(
                puzzle = puzzle,
                onThemeChanged = { ThemeChanged(it) },
            )
        }
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
