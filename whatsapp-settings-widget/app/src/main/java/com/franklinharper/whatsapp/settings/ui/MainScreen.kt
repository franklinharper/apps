package com.franklinharper.whatsapp.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.franklinharper.whatsapp.settings.MainUiState
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onOpenSettingsClick: () -> Unit,
) {
    WhatsAppAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("WhatsApp Background Activity") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (uiState.status) {
                    WhatsAppStatus.BackgroundUsageUnrestricted -> {
                        StatusLabel(
                            text = "Background usage: Enabled",
                            backgroundColor = Color.Red,
                        )
                    }
                    WhatsAppStatus.BackgroundUsageUnknown -> {
                        StatusLabel(
                            text = "Background usage: Disabled",
                            backgroundColor = Color.Green,
                        )
                    }
                    WhatsAppStatus.NotInstalled -> {
                        Text(
                            text = "WhatsApp not installed",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onOpenSettingsClick,
                    enabled = uiState.status != WhatsAppStatus.NotInstalled,
                ) {
                    Text("Open WhatsApp Battery Settings")
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(
    text: String,
    backgroundColor: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
