package com.franklinharper.whatsapp.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WaGreen = Color(0xFF25D366)
private val WaDarkGreen = Color(0xFF128C7E)
private val WaRed = Color(0xFFE53935)
private val WaWhite = Color.White

private val WhatsAppColorScheme = lightColorScheme(
    primary = WaGreen,
    onPrimary = WaWhite,
    primaryContainer = WaDarkGreen,
    onPrimaryContainer = WaWhite,
    error = WaRed,
)

@Composable
fun WhatsAppAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WhatsAppColorScheme,
        content = content,
    )
}
