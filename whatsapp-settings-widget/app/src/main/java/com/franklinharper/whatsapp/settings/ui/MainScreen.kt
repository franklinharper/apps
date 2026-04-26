package com.franklinharper.whatsapp.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val (icon, iconTint, label) = statusDisplay(uiState.status)

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(72.dp),
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = iconTint,
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

private data class StatusDisplay(
    val icon: ImageVector,
    val tint: Color,
    val label: String,
)

private fun statusDisplay(status: WhatsAppStatus): StatusDisplay = when (status) {
    WhatsAppStatus.BackgroundUsageUnrestricted -> StatusDisplay(
        icon = Icons.Filled.CheckCircle,
        tint = Color(0xFF25D366),
        label = "Background usage: Unrestricted",
    )
    WhatsAppStatus.BackgroundUsageUnknown -> StatusDisplay(
        icon = Icons.Filled.Warning,
        tint = Color(0xFFF57C00),
        label = "Background usage: Unknown",
    )
    WhatsAppStatus.NotInstalled -> StatusDisplay(
        icon = Icons.Filled.PhoneDisabled,
        tint = Color(0xFF9E9E9E),
        label = "WhatsApp not installed",
    )
}
