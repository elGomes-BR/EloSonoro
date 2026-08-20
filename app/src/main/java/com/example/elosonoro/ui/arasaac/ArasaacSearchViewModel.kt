package com.example.elosonoro.ui.arasaac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elosonoro.domain.repository.ArasaacRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para pesquisa e seleção de pictogramas da API ARASAAC.
 */
class ArasaacSearchViewModel(
    private val arasaacRepository: ArasaacRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArasaacSearchUiState())
    val uiState: StateFlow<ArasaacSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun searchWithDebounce(query: String, delayMs: Long = 400L) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(query = query)

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                pictograms = emptyList(),
                isLoading = false,
                errorMessage = null
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(delayMs)
            executeSearch(query)
        }
    }

    fun searchImmediately(query: String) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(query = query)
        viewModelScope.launch {
            executeSearch(query)
        }
    }

    private suspend fun executeSearch(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val result = arasaacRepository.searchPictograms(term = query, locale = "pt")

        result.onSuccess { list ->
            _uiState.value = _uiState.value.copy(
                pictograms = list,
                isLoading = false,
                errorMessage = if (list.isEmpty()) "Nenhum pictograma encontrado para '$query'" else null
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                pictograms = emptyList(),
                isLoading = false,
                errorMessage = "Falha ao conectar com o serviço ARASAAC: ${error.message}"
            )
        }
    }

    fun downloadAndSelectPictogram(pictogramId: Int, onComplete: (savedLocalPath: String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = arasaacRepository.downloadAndSavePictogram(pictogramId)

            result.onSuccess { localPath ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedImagePath = localPath
                )
                onComplete(localPath)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Erro ao baixar pictograma: ${error.message}"
                )
                onComplete(null)
            }
        }
    }
}
