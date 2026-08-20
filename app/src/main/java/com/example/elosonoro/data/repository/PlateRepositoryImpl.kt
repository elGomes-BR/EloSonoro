package com.example.elosonoro.data.repository

import android.net.Uri
import android.util.Log
import com.example.elosonoro.data.local.dao.PlateDao
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.domain.repository.PlateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID

/**
 * Implementação do repositório de Placas.
 * Conforme constitution.md e spec.md (Seção 2.4):
 * - Move/copia mídias temporárias para o armazenamento privado do aplicativo (filesDir).
 * - Registra caminhos absolutos no banco Room (sem BLOBs).
 * - Bloqueia submissão de placas com nome vazio.
 */
class PlateRepositoryImpl(
    private val plateDao: PlateDao,
    private val fileManager: FileManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PlateRepository {

    companion object {
        private const val TAG = "PlateRepository"
    }

    override fun getAllPlates(): Flow<List<PlateEntity>> = plateDao.getAllPlates()

    override fun getPlateById(id: String): Flow<PlateEntity?> = plateDao.getPlateById(id)

    override suspend fun getPlateByIdSync(id: String): PlateEntity? = withContext(ioDispatcher) {
        plateDao.getPlateByIdSync(id)
    }

    override fun searchPlates(query: String): Flow<List<PlateEntity>> = plateDao.searchPlatesByName(query)

    override suspend fun savePlate(
        name: String,
        imageUri: String?,
        audioUri: String?,
        id: String?
    ): Result<PlateEntity> = withContext(ioDispatcher) {
        if (name.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("O nome da placa é obrigatório e não pode ser vazio."))
        }

        try {
            // Processamento físico da imagem
            val permanentImagePath = processMediaImage(imageUri)

            // Processamento físico do áudio
            val permanentAudioPath = processMediaAudio(audioUri)

            val plateEntity = PlateEntity(
                id = id?.ifBlank { null } ?: UUID.randomUUID().toString(),
                name = name.trim(),
                imageUri = permanentImagePath,
                audioUri = permanentAudioPath
            )

            plateDao.insert(plateEntity)
            Log.d(TAG, "Placa salva com sucesso: ${plateEntity.name} (id: ${plateEntity.id})")
            Result.success(plateEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar placa: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePlate(plate: PlateEntity): Result<Unit> = withContext(ioDispatcher) {
        try {
            plateDao.delete(plate)

            // Limpeza física dos arquivos de mídia associados
            plate.imageUri?.let { path ->
                fileManager.deleteFile(path)
            }
            plate.audioUri?.let { path ->
                fileManager.deleteFile(path)
            }

            Log.d(TAG, "Placa excluída com sucesso: ${plate.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir placa: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun processMediaImage(sourceUriString: String?): String? {
        if (sourceUriString.isNullOrBlank()) return null

        val imagesDir = fileManager.getImagesDirectory()

        // Se já for um arquivo no diretório interno de imagens, mantém o path
        if (sourceUriString.contains(imagesDir.absolutePath) && fileManager.fileExists(sourceUriString)) {
            return sourceUriString
        }

        return try {
            fileManager.saveImage(sourceUriString)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao processar arquivo de imagem $sourceUriString: ${e.message}")
            sourceUriString
        }
    }

    private fun processMediaAudio(sourceUriString: String?): String? {
        if (sourceUriString.isNullOrBlank()) return null

        val audioDir = fileManager.getAudioDirectory()

        // Se já for um arquivo no diretório interno de áudio, mantém o path
        if (sourceUriString.contains(audioDir.absolutePath) && fileManager.fileExists(sourceUriString)) {
            return sourceUriString
        }

        return try {
            fileManager.saveAudio(sourceUriString)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao processar arquivo de áudio $sourceUriString: ${e.message}")
            sourceUriString
        }
    }
}
