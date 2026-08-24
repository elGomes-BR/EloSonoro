package com.example.elosonoro.domain.usecase

import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.domain.repository.ProfileRepository

/**
 * Caso de uso para atualização de Categoria/Perfil com unicidade de nome e sincronização de placas associadas.
 * Conforme spec.md (Seção 2.5) e plan.md (Seção 7.4.2).
 */
class UpdateProfileUseCase(
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        profile: ProfileEntity,
        plateIds: List<String>? = null
    ): Result<Unit> {
        val trimmedName = profile.name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("O nome da categoria é obrigatório e não pode ser vazio."))
        }
        return profileRepository.updateProfile(
            profile = profile.copy(name = trimmedName),
            plateIds = plateIds
        )
    }
}
