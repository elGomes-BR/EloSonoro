package com.example.elosonoro.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserPreferences {
    val spanCountFlow: StateFlow<Int>
    val isEditModeFlow: StateFlow<Boolean>
    val selectedProfileIdFlow: StateFlow<String?>
    fun getSpanCount(): Int
    fun setSpanCount(spanCount: Int)
    fun isEditMode(): Boolean
    fun setEditMode(isEditMode: Boolean)
    fun toggleEditMode(): Boolean
    fun getSelectedProfileId(): String?
    fun setSelectedProfileId(profileId: String?)
}

/**
 * Gerenciador de preferências do aplicativo.
 * Conforme spec.md (Seção 1.3 e 2.6):
 * - Controla o modo de operação (Modo de Uso vs Modo de Edição).
 * - Controla a quantidade de colunas na grade (spanCount: 2, 4, 8, 16).
 * - Mantém a seleção da categoria ativa.
 */
class PreferencesManager(
    context: Context? = null,
    injectedPrefs: SharedPreferences? = null
) : UserPreferences {

    companion object {
        private const val PREFS_NAME = "elo_sonoro_prefs"
        private const val KEY_SPAN_COUNT = "key_span_count"
        private const val KEY_IS_EDIT_MODE = "key_is_edit_mode"
        private const val KEY_SELECTED_PROFILE_ID = "key_selected_profile_id"

        const val DEFAULT_SPAN_COUNT = 4
    }

    private val sharedPreferences: SharedPreferences =
        injectedPrefs ?: context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?: error("Context ou SharedPreferences deve ser fornecido para PreferencesManager")

    private val _spanCount = MutableStateFlow(
        sharedPreferences.getInt(KEY_SPAN_COUNT, DEFAULT_SPAN_COUNT)
    )
    override val spanCountFlow: StateFlow<Int> = _spanCount.asStateFlow()

    private val _isEditMode = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_IS_EDIT_MODE, false)
    )
    override val isEditModeFlow: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedProfileId = MutableStateFlow(
        sharedPreferences.getString(KEY_SELECTED_PROFILE_ID, null)
    )
    override val selectedProfileIdFlow: StateFlow<String?> = _selectedProfileId.asStateFlow()

    override fun getSpanCount(): Int = _spanCount.value

    override fun setSpanCount(spanCount: Int) {
        val validSpan = if (spanCount in listOf(2, 4, 6)) spanCount else DEFAULT_SPAN_COUNT
        sharedPreferences.edit().putInt(KEY_SPAN_COUNT, validSpan).apply()
        _spanCount.value = validSpan
    }

    override fun isEditMode(): Boolean = _isEditMode.value

    override fun setEditMode(isEditMode: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_EDIT_MODE, isEditMode).apply()
        _isEditMode.value = isEditMode
    }

    override fun toggleEditMode(): Boolean {
        val newMode = !_isEditMode.value
        setEditMode(newMode)
        return newMode
    }

    override fun getSelectedProfileId(): String? = _selectedProfileId.value

    override fun setSelectedProfileId(profileId: String?) {
        sharedPreferences.edit().putString(KEY_SELECTED_PROFILE_ID, profileId).apply()
        _selectedProfileId.value = profileId
    }
}
