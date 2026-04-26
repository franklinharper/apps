package com.franklinharper.whatsapp.settings.intent

import org.junit.Assert.assertEquals
import org.junit.Test
import android.os.Build

class SettingsIntentBuilderTest {

    @Test
    fun `on API 33+ selects battery settings path`() {
        val builder = SettingsIntentBuilder(sdkInt = Build.VERSION_CODES.TIRAMISU)
        assertEquals(IntentPath.BatterySettings, builder.selectPath())
    }

    @Test
    fun `on API 23 selects app details path`() {
        val builder = SettingsIntentBuilder(sdkInt = Build.VERSION_CODES.M)
        assertEquals(IntentPath.AppDetailsSettings, builder.selectPath())
    }

    @Test
    fun `default constructor uses real SDK_INT (smoke test)`() {
        // Just verifies it doesn't crash
        val builder = SettingsIntentBuilder()
        builder.selectPath()
    }
}
