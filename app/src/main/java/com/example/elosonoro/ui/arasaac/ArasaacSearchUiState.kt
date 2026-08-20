package com.example.elosonoro.ui.arasaac

import com.example.elosonoro.domain.model.ArasaacPictogram

/**
 * Estado imutável do diálogo de pesquisa da API ARASAAC.
 */
data class ArasaacSearchUiState(
    val query: String = "",
    val pictograms: List<ArasaacPictogram> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedImagePath: String? = null
)
