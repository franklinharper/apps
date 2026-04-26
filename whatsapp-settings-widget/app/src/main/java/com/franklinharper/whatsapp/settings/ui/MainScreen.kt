package com.franklinharper.whatsapp.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.franklinharper.whatsapp.settings.MainUiState
import com.franklinharper.whatsapp.settings.R
import com.franklinharper.whatsapp.settings.domain.WhatsAppStatus
import com.franklinharper.whatsapp.settings.presentation.UnrestrictedSessionUiModel
import com.franklinharper.whatsapp.settings.presentation.toDisplay

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
                    title = { Text(stringResource(R.string.screen_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    CurrentStatusSection(
                        uiState = uiState,
                        onOpenSettingsClick = onOpenSettingsClick,
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.sessions_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }

                if (uiState.unrestrictedSessions.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.sessions_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                        )
                    }
                } else {
                    items(
                        items = uiState.unrestrictedSessions,
                        key = { it.id },
                    ) { session ->
                        UnrestrictedSessionRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentStatusSection(
    uiState: MainUiState,
    onOpenSettingsClick: () -> Unit,
) {
    val display = uiState.status.toDisplay()
    val bgColor = if (display.unrestricted) StatusEnabledColor else StatusDisabledColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(display.labelRes),
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
            Text(stringResource(R.string.open_settings_button))
        }
    }
}

@Composable
private fun UnrestrictedSessionRow(session: UnrestrictedSessionUiModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.header,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = session.timeRange,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = session.duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
