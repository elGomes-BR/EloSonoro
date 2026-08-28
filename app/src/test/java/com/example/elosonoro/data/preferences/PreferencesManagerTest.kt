package com.example.elosonoro.data.preferences

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreferencesManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        preferencesManager = PreferencesManager(injectedPrefs = fakePrefs)
    }

    @Test
    fun testSetSpanCountAcceptsOnlyTwoFourOrSixColumns() {
        // Inicial padrão é 4
        assertEquals(4, preferencesManager.getSpanCount())

        // 2 colunas é válido
        preferencesManager.setSpanCount(2)
        assertEquals(2, preferencesManager.getSpanCount())
        assertEquals(2, fakePrefs.getInt("key_span_count", 4))

        // 4 colunas é válido
        preferencesManager.setSpanCount(4)
        assertEquals(4, preferencesManager.getSpanCount())
        assertEquals(4, fakePrefs.getInt("key_span_count", 4))

        // 6 colunas é válido
        preferencesManager.setSpanCount(6)
        assertEquals(6, preferencesManager.getSpanCount())
        assertEquals(6, fakePrefs.getInt("key_span_count", 4))

        // Outros valores (ex: 8, 16, 5) caem estritamente no valor padrão (4)
        preferencesManager.setSpanCount(8)
        assertEquals(4, preferencesManager.getSpanCount())

        preferencesManager.setSpanCount(16)
        assertEquals(4, preferencesManager.getSpanCount())

        preferencesManager.setSpanCount(5)
        assertEquals(4, preferencesManager.getSpanCount())
    }

    @Test
    fun testToggleEditMode() {
        // Inicial padrão é false (Modo de Uso)
        assertFalse(preferencesManager.isEditMode())

        val newMode = preferencesManager.toggleEditMode()
        assertTrue(newMode)
        assertTrue(preferencesManager.isEditMode())
        assertTrue(fakePrefs.getBoolean("key_is_edit_mode", false))

        val secondMode = preferencesManager.toggleEditMode()
        assertFalse(secondMode)
        assertFalse(preferencesManager.isEditMode())
        assertFalse(fakePrefs.getBoolean("key_is_edit_mode", false))
    }

    @Test
    fun testSetSelectedProfileId() {
        preferencesManager.setSelectedProfileId("profile-123")
        assertEquals("profile-123", preferencesManager.getSelectedProfileId())
        assertEquals("profile-123", fakePrefs.getString("key_selected_profile_id", null))

        preferencesManager.setSelectedProfileId(null)
        assertEquals(null, preferencesManager.getSelectedProfileId())
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val storage = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = storage
        override fun getString(key: String?, defValue: String?): String? = storage[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (storage[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = storage[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = storage[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = storage[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = storage[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = storage.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(storage)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) temp[key] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) temp.remove(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                temp.clear()
                return this
            }
            override fun commit(): Boolean {
                storage.putAll(temp)
                return true
            }
            override fun apply() {
                storage.putAll(temp)
            }
        }
    }
}
