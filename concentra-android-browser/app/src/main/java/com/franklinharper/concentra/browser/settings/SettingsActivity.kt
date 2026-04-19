package com.franklinharper.concentra.browser.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.collectAsState

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsApp(activity = this)
        }
    }
}

@Composable
private fun SettingsApp(activity: SettingsActivity) {
    val repository =
        remember(activity) {
            PreferencesSettingsRepository(
                sharedPreferences =
                    activity.applicationContext.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE,
                    ),
            )
        }
    val viewModel: SettingsViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        SettingsViewModel(settingsRepository = repository)
                    }
                },
        )
    val uiState by viewModel.uiState.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SettingsScreen(
                uiState = uiState,
                onBackClick = activity::finish,
                onThirdPartyCookiesChanged = viewModel::onThirdPartyCookiesChanged,
            )
        }
    }
}

private const val PREFERENCES_NAME = "browser_settings"
