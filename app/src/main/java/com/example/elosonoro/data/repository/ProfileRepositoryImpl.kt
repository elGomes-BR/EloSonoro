package com.example.elosonoro.data.repository

import android.util.Log
import com.example.elosonoro.data.local.dao.CrossRefDao
import com.example.elosonoro.data.local.dao.ProfileDao
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.model.ProfileWithPlates
import com.example.elosonoro.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Implementação do repositório de Perfis/Categorias.
 * Conforme spec.md (Seção 1.2 e 2.5):
 * - Garante unicidade estrita de nomes de categorias com erro amigável/explícito.
 * - Associa placas no ato da criação (PlateProfileCrossRef).
 * - Exclusão de categoria não deleta as placas associadas (sem cascata destrutiva de placas).
 */
class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val crossRefDao: CrossRefDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProfileRepository {

    companion object {
        private const val TAG = "ProfileRepository"
    }

    override fun getAllProfiles(): Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    override fun getProfileById(id: String): Flow<ProfileEntity?> = profileDao.getProfileById(id)

    override fun getAllProfilesWithPlates(): Flow<List<ProfileWithPlates>> = profileDao.getAllProfilesWithPlates()

    override fun getProfileWithPlates(profileId: String): Flow<ProfileWithPlates?> =
        profileDao.getProfileWithPlates(profileId)

    override fun getPlatesForProfile(profileId: String): Flow<List<PlateEntity>> =
        crossRefDao.getPlatesForProfile(profileId)

    override suspend fun createProfile(
        name: String,
        plateIds: List<String>
    ): Result<ProfileEntity> = withContext(ioDispatcher) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("O nome da categoria é obrigatório e não pode ser vazio.")
            )
        }

        try {
            // Verificação de unicidade de nome
            val existing = profileDao.getProfileByNameSync(trimmedName)
            if (existing != null) {
                return@withContext Result.failure(
                    IllegalStateException("Já existe uma categoria cadastrada com o nome '$trimmedName'.")
                )
            }

            val newProfile = ProfileEntity(
                id = UUID.randomUUID().toString(),
                name = trimmedName
            )

            profileDao.insert(newProfile)

            // Associação imediata de placas selecionadas
            if (plateIds.isNotEmpty()) {
                val crossRefs = plateIds.map { plateId ->
                    PlateProfileCrossRef(
                        plateId = plateId,
                        profileId = newProfile.id
                    )
                }
                crossRefDao.insertAll(crossRefs)
                Log.d(TAG, "Categoria '${newProfile.name}' criada com ${crossRefs.size} placas vinculadas.")
            } else {
                Log.d(TAG, "Categoria '${newProfile.name}' criada sem placas vinculadas.")
            }

            Result.success(newProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar perfil '$trimmedName': ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        profile: ProfileEntity,
        plateIds: List<String>?
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            profileDao.update(profile)

            if (plateIds != null) {
                // Atualiza vínculos: remove anteriores e insere novos
                crossRefDao.deleteAllPlatesFromProfile(profile.id)
                if (plateIds.isNotEmpty()) {
                    val crossRefs = plateIds.map { plateId ->
                        PlateProfileCrossRef(
                            plateId = plateId,
                            profileId = profile.id
                        )
                    }
                    crossRefDao.insertAll(crossRefs)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar perfil '${profile.name}': ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteProfile(profile: ProfileEntity): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Remove o perfil. Pela constraint ForeignKey.CASCADE na tabela associativa,
            // os vínculos são excluídos sem afetar as entidades de placa (sem cascata destrutiva em placas).
            profileDao.delete(profile)
            Log.d(TAG, "Categoria '${profile.name}' excluída com sucesso.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir categoria '${profile.name}': ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun addPlateToProfile(profileId: String, plateId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            crossRefDao.insert(
                PlateProfileCrossRef(
                    plateId = plateId,
                    profileId = profileId
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao vincular placa $plateId ao perfil $profileId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun removePlateFromProfile(profileId: String, plateId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            crossRefDao.deleteRelation(profileId = profileId, plateId = plateId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desvincular placa $plateId do perfil $profileId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
