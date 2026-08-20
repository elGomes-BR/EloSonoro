package com.example.elosonoro.domain.repository

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.model.ProfileWithPlates
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositório para gerenciamento de Perfis/Categorias de comunicação.
 */
interface ProfileRepository {

    fun getAllProfiles(): Flow<List<ProfileEntity>>

    fun getProfileById(id: String): Flow<ProfileEntity?>

    fun getAllProfilesWithPlates(): Flow<List<ProfileWithPlates>>

    fun getProfileWithPlates(profileId: String): Flow<ProfileWithPlates?>

    fun getPlatesForProfile(profileId: String): Flow<List<PlateEntity>>

    /**
     * Cria um novo perfil de categoria com verificação estrita de unicidade de nome e
     * associação imediata de placas no ato da criação.
     * @param name Nome do perfil (obrigatório, não pode ser vazio e deve ser único).
     * @param plateIds Lista de IDs de placas para associação imediata.
     */
    suspend fun createProfile(name: String, plateIds: List<String> = emptyList()): Result<ProfileEntity>

    /**
     * Atualiza dados de um perfil e sincroniza suas placas associadas se fornecidas.
     */
    suspend fun updateProfile(profile: ProfileEntity, plateIds: List<String>? = null): Result<Unit>

    /**
     * Remove o perfil do banco sem excluir as placas associadas.
     */
    suspend fun deleteProfile(profile: ProfileEntity): Result<Unit>

    /**
     * Adiciona uma placa à categoria especificada.
     */
    suspend fun addPlateToProfile(profileId: String, plateId: String): Result<Unit>

    /**
     * Remove o vínculo de uma placa com a categoria especificada.
     */
    suspend fun removePlateFromProfile(profileId: String, plateId: String): Result<Unit>
}
