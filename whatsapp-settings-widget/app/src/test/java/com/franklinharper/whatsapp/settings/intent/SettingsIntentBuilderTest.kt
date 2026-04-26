package com.franklinharper.whatsapp.settings.intent

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SettingsIntentBuilderTest {

    @Config(sdk = [33])
    @Test
    fun `on API 33 builds ACTION_APP_BATTERY_SETTINGS with package extra`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = SettingsIntentBuilder().build(context)
        assertEquals(Settings.ACTION_APP_BATTERY_SETTINGS, intent.action)
        assertEquals("com.whatsapp", intent.getStringExtra("package"))
    }

    @Config(sdk = [23])
    @Test
    fun `on API 23 builds ACTION_APPLICATION_DETAILS_SETTINGS with package uri`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = SettingsIntentBuilder().build(context)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals(Uri.fromParts("package", "com.whatsapp", null), intent.data)
    }

    @Config(sdk = [23])
    @Test
    fun `intent includes FLAG_ACTIVITY_NEW_TASK`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = SettingsIntentBuilder().build(context)
        assert(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
