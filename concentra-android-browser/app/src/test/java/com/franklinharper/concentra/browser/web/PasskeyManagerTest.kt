package com.franklinharper.concentra.browser.web

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyManagerTest {

    @Test
    fun `isSupported returns true on API 26`() {
        assertTrue(PasskeyManager(sdkInt = 26).isSupported())
    }

    @Test
    fun `isSupported returns true on API 34`() {
        assertTrue(PasskeyManager(sdkInt = 34).isSupported())
    }

    @Test
    fun `isSupported returns true on API 23`() {
        assertTrue(PasskeyManager(sdkInt = 23).isSupported())
    }

    @Test
    fun `isSupported returns false on API 22`() {
        assertFalse(PasskeyManager(sdkInt = 22).isSupported())
    }

    @Test
    fun `create returns NotSupported on API 22`() {
        val manager = PasskeyManager(sdkInt = 22)
        val result = manager.create(
            context = ApplicationProvider.getApplicationContext(),
            requestJson = "{}",
            origin = "https://example.com",
        )
        assertEquals(PasskeyResult.NotSupported, result)
    }

    @Test
    fun `get returns NotSupported on API 22`() {
        val manager = PasskeyManager(sdkInt = 22)
        val result = manager.get(
            context = ApplicationProvider.getApplicationContext(),
            requestJson = "{}",
            origin = "https://example.com",
        )
        assertEquals(PasskeyResult.NotSupported, result)
    }
}
