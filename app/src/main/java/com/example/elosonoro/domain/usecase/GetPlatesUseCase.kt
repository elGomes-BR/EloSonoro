package com.example.elosonoro.domain.usecase

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.domain.repository.PlateRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para recuperação e busca de Placas.
 */
class GetPlatesUseCase(
    private val plateRepository: PlateRepository
) {

    operator fun invoke(query: String = ""): Flow<List<PlateEntity>> {
        return if (query.isBlank()) {
            plateRepository.getAllPlates()
        } else {
            plateRepository.searchPlates(query.trim())
        }
    }
}
