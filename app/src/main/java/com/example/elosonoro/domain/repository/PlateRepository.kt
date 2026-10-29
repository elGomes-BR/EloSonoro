package com.example.elosonoro.domain.repository

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositório para gerenciamento de Placas de comunicação.
 */
interface PlateRepository {

    fun getAllPlates(): Flow<List<PlateEntity>>

    fun getPlateById(id: String): Flow<PlateEntity?>

    suspend fun getPlateByIdSync(id: String): PlateEntity?

    fun searchPlates(query: String): Flow<List<PlateEntity>>

    /**
     * Salva uma placa garantindo a persistência física das mídias antes do registro no banco.
     * @param name Nome da placa (obrigatório, não pode ser vazio).
     * @param imageUri URI ou caminho da imagem (opcional). Se for externo/temporário, será copiado para filesDir.
     * @param audioUri URI ou caminho do áudio (opcional). Se for externo/temporário, será copiado para filesDir.
     * @param id ID opcional da placa (se nulo, um novo UUID é gerado).
     * @param categoryIds Lista opcional de IDs de categorias para vínculo transacional via PlateProfileCrossRef.
     */
    suspend fun savePlate(
        name: String,
        imageUri: String? = null,
        audioUri: String? = null,
        id: String? = null,
        categoryIds: List<String>? = null
    ): Result<PlateEntity>

    /**
     * Retorna as categorias associadas a uma determinada placa.
     */
    fun getProfilesForPlate(plateId: String): Flow<List<ProfileEntity>>

    /**
     * Remove a placa do banco de dados e apaga seus arquivos físicos de mídia correspondentes.
     */
    suspend fun deletePlate(plate: PlateEntity): Result<Unit>

    /**
     * Remove a placa pelo seu ID do banco de dados e apaga seus arquivos físicos de mídia correspondentes.
     */
    suspend fun deletePlateById(id: String): Result<Unit>
}
