package com.example.elosonoro.domain.usecase

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.domain.repository.PlateRepository
import com.example.elosonoro.util.formatPlateName

/**
 * Caso de uso para validação e persistência segura de Placas.
 */
class SavePlateUseCase(
    private val plateRepository: PlateRepository
) {

    suspend operator fun invoke(
        name: String,
        imageUri: String? = null,
        audioUri: String? = null,
        id: String? = null
    ): Result<PlateEntity> {
        val formattedName = name.formatPlateName()
        if (formattedName.isBlank()) {
            return Result.failure(IllegalArgumentException("O nome da placa é obrigatório e não pode ser vazio."))
        }
        return plateRepository.savePlate(
            name = formattedName,
            imageUri = imageUri,
            audioUri = audioUri,
            id = id
        )
    }
}
