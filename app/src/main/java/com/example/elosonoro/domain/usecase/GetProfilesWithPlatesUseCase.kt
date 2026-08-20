package com.example.elosonoro.domain.usecase

import com.example.elosonoro.data.local.model.ProfileWithPlates
import com.example.elosonoro.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para obter os perfis/categorias estruturados com suas respectivas placas associadas.
 */
class GetProfilesWithPlatesUseCase(
    private val profileRepository: ProfileRepository
) {

    operator fun invoke(): Flow<List<ProfileWithPlates>> {
        return profileRepository.getAllProfilesWithPlates()
    }
}
