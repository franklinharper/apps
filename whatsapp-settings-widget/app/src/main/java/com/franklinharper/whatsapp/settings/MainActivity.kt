package com.franklinharper.whatsapp.settings

import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.franklinharper.whatsapp.settings.domain.SystemWhatsAppStatusRepository
import com.franklinharper.whatsapp.settings.intent.SettingsIntentBuilder
import com.franklinharper.whatsapp.settings.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = SystemWhatsAppStatusRepository(
                    packageManager = packageManager,
                    powerManager = getSystemService(PowerManager::class.java),
                )
                return MainViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MainScreen(
                uiState = uiState,
                onOpenSettingsClick = {
                    startActivity(SettingsIntentBuilder().build(this))
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
