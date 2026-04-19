package com.franklinharper.concentra.browser.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DebugViewModel(
    sdkInt: Int = Build.VERSION.SDK_INT,
) : ViewModel() {
    val uiState: StateFlow<DebugUiState> = MutableStateFlow(
        DebugUiState(
            apiLevel = sdkInt,
            passkeysSupported = sdkInt >= 26,
            passkeysUnsupportedReason = if (sdkInt >= 26) null else "Requires API 26, running API $sdkInt",
        ),
    ).asStateFlow()
}

data class DebugUiState(
    val apiLevel: Int,
    val passkeysSupported: Boolean,
    val passkeysUnsupportedReason: String?,
)
