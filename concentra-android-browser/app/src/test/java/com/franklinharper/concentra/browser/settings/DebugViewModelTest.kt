package com.franklinharper.concentra.browser.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugViewModelTest {

    @Test
    fun `uiState reflects the given API level`() {
        val viewModel = DebugViewModel(sdkInt = 30)
        assertEquals(30, viewModel.uiState.value.apiLevel)
    }

    @Test
    fun `passkeys supported on API 26`() {
        val viewModel = DebugViewModel(sdkInt = 26)
        assertTrue(viewModel.uiState.value.passkeysSupported)
        assertNull(viewModel.uiState.value.passkeysUnsupportedReason)
    }

    @Test
    fun `passkeys supported on API 34`() {
        val viewModel = DebugViewModel(sdkInt = 34)
        assertTrue(viewModel.uiState.value.passkeysSupported)
        assertNull(viewModel.uiState.value.passkeysUnsupportedReason)
    }

    @Test
    fun `passkeys not supported on API 25`() {
        val viewModel = DebugViewModel(sdkInt = 25)
        assertFalse(viewModel.uiState.value.passkeysSupported)
        assertNotNull(viewModel.uiState.value.passkeysUnsupportedReason)
    }

    @Test
    fun `unsupported reason mentions the API level`() {
        val viewModel = DebugViewModel(sdkInt = 23)
        assertTrue(viewModel.uiState.value.passkeysUnsupportedReason!!.contains("23"))
    }
}
