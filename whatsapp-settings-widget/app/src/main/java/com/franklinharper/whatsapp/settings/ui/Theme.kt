package com.franklinharper.whatsapp.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WhatsAppColorScheme = lightColorScheme(
    primary = StatusDisabledColor,
    onPrimary = StatusTextColor,
    primaryContainer = Color(0xFF128C7E),
    onPrimaryContainer = StatusTextColor,
    error = StatusEnabledColor,
)

@Composable
fun WhatsAppAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WhatsAppColorScheme,
        content = content,
    )
}
