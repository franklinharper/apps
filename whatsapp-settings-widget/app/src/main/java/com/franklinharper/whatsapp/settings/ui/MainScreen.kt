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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.franklinharper.whatsapp.settings.MainUiState
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.domain.toDisplay

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
                val display = uiState.status.toDisplay()
                val bgColor = if (display.enabled) StatusEnabledColor else StatusDisabledColor

                Text(
                    text = display.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = StatusTextColor,
                    modifier = Modifier
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

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
