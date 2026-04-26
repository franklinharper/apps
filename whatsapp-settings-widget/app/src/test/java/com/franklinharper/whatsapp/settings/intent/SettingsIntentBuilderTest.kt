package com.franklinharper.whatsapp.settings.intent

import android.provider.Settings
import com.franklinharper.whatsapp.settings.domain.WhatsAppPackage
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsIntentBuilderTest {

    @Test
    fun `spec opens WhatsApp application details settings`() {
        val spec = SettingsIntentBuilder().spec()

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, spec.action)
        assertEquals(WhatsAppPackage.REGULAR, spec.packageName)
    }
}
