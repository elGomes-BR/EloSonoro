package com.example.elosonoro.domain.usecase

import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.domain.repository.ProfileRepository

/**
 * Caso de uso para criação de Categoria/Perfil com unicidade de nome e vínculo imediato de placas.
 */
class CreateProfileUseCase(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        name: String,
        plateIds: List<String> = emptyList()
    ): Result<ProfileEntity> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("O nome da categoria é obrigatório e não pode ser vazio."))
        }
        return profileRepository.createProfile(
            name = name,
            plateIds = plateIds
        )
    }
}
