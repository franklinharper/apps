package com.franklinharper.concentra.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.franklinharper.concentra.browser.ui.BrowserRoute

class BrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrowserApp(activity = this)
        }
    }
}

@Composable
private fun BrowserApp(activity: BrowserActivity) {
    val container = remember(activity) { BrowserAppContainer(activity) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BrowserRoute(container = container)
        }
    }
}
