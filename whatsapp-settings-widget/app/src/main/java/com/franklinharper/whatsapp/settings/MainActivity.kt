package com.franklinharper.whatsapp.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatusRepositoryFactory
import com.franklinharper.whatsapp.settings.intent.SettingsIntentBuilder
import com.franklinharper.whatsapp.settings.ui.MainScreen
import com.franklinharper.whatsapp.settings.widget.StatusWidgetUpdater
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(WhatsAppStatusRepositoryFactory.create(this@MainActivity)) as T
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
                    startActivity(SettingsIntentBuilder().build())
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        lifecycleScope.launch {
            StatusWidgetUpdater.refresh(
                context = this@MainActivity,
                status = viewModel.uiState.value.status,
            )
        }
    }
}
