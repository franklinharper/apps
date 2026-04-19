package com.franklinharper.concentra.browser.web

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `isSupported returns false on API 25`() {
        assertFalse(PasskeyManager(sdkInt = 25).isSupported())
    }

    @Test
    fun `isSupported returns false on API 23`() {
        assertFalse(PasskeyManager(sdkInt = 23).isSupported())
    }

    @Test
    fun `create returns NotSupported on API 25`() = runTest {
        val manager = PasskeyManager(sdkInt = 25)
        val result = manager.create(activity = null, requestJson = "{}", origin = "https://example.com")
        assertEquals(PasskeyResult.NotSupported, result)
    }

    @Test
    fun `get returns NotSupported on API 25`() = runTest {
        val manager = PasskeyManager(sdkInt = 25)
        val result = manager.get(activity = null, requestJson = "{}", origin = "https://example.com")
        assertEquals(PasskeyResult.NotSupported, result)
    }
}
