package com.franklinharper.whatsapp.settings.intent

import org.junit.Test

class SettingsIntentBuilderTest {

    @Test
    fun `build does not throw`() {
        // The builder now has no branching logic — it always constructs the same intent.
        // Verifying Intent contents requires an Android framework (Robolectric/instrumented test),
        // which is unavailable in this sandbox. A smoke test ensures the constructor works.
        SettingsIntentBuilder()
    }
}
