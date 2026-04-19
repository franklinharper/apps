package com.franklinharper.concentra.browser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.franklinharper.concentra.browser.model.BrowserUiState

@Composable
fun BrowserChromeSheet(
    uiState: BrowserUiState,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onUrlSubmit: () -> Unit,
    onGoogleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onShareClick: () -> Unit,
    onFindClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .imePadding()
                .testTag(BrowserScreenTags.ChromeSheet),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlInputChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(BrowserScreenTags.UrlField),
                label = { Text("Address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onUrlSubmit() }),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onUrlSubmit) {
                    Text("Go")
                }
                OutlinedButton(onClick = onGoogleClick, enabled = uiState.isGoogleEnabled) {
                    Text("Google")
                }
                OutlinedButton(onClick = onArchiveClick, enabled = uiState.isArchiveTodayEnabled) {
                    Text("Archive")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onShareClick, enabled = uiState.isShareEnabled) {
                    Text("Share")
                }
                OutlinedButton(onClick = onFindClick, enabled = uiState.isFindInPageEnabled) {
                    Text("Find")
                }
                OutlinedButton(onClick = onSettingsClick) {
                    Text("Settings")
                }
                OutlinedButton(onClick = onExitClick) {
                    Text("Exit")
                }
            }
        }
    }
}
