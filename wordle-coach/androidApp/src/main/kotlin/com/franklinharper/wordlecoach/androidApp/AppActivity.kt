package com.franklinharper.wordlecoach.androidApp

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

        var puzzle by mutableStateOf<PuzzleResult?>(null)
        var imageBitmap by mutableStateOf<ImageBitmap?>(null)

        @Suppress("DEPRECATION")
        val imageUri: Uri? = if (intent?.action == Intent.ACTION_SEND)
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        else
            null

        if (imageUri != null) {
            lifecycleScope.launch {
                val (parsedPuzzle, bitmap) = withContext(Dispatchers.IO) {
                    val parsed = WordleImageParser(this@AppActivity).parse(imageUri)
                    val bmp = contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                    parsed to bmp
                }
                puzzle = parsedPuzzle
                imageBitmap = bitmap
            }
        }

        setContent {
            App(
                puzzle = puzzle,
                imageBitmap = imageBitmap,
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
