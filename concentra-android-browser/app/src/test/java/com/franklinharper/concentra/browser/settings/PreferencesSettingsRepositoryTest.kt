package com.franklinharper.concentra.browser.settings

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesSettingsRepositoryTest {
    @Test
    fun `third party cookies default to disabled`() {
        val repo = PreferencesSettingsRepository(fakeSharedPreferences())

        assertFalse(repo.load().thirdPartyCookiesEnabled)
    }

    @Test
    fun `saving third party cookies enabled persists value`() {
        val repo = PreferencesSettingsRepository(fakeSharedPreferences())

        repo.saveThirdPartyCookiesEnabled(true)

        assertTrue(repo.load().thirdPartyCookiesEnabled)
    }

    private fun fakeSharedPreferences(): SharedPreferences = InMemorySharedPreferences()
}

private class InMemorySharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (values[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    private inner class Editor : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            values[key.orEmpty()] = value
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            this@InMemorySharedPreferences.values[key.orEmpty()] = values
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            this@InMemorySharedPreferences.values[key.orEmpty()] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            this@InMemorySharedPreferences.values[key.orEmpty()] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            this@InMemorySharedPreferences.values[key.orEmpty()] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            this@InMemorySharedPreferences.values[key.orEmpty()] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            values.remove(key.orEmpty())
        }

        override fun clear(): SharedPreferences.Editor = apply {
            values.clear()
        }

        override fun commit(): Boolean = true

        override fun apply() = Unit
    }
}
