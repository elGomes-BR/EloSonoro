package com.example.elosonoro.ui.board

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.ProfileEntity

/**
 * Estado imutável da tela principal de prancha de comunicação.
 */
data class BoardUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val selectedProfileId: String? = null,
    val plates: List<PlateEntity> = emptyList(),
    val isEditMode: Boolean = false,
    val spanCount: Int = 4,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val userMessage: String? = null
)
