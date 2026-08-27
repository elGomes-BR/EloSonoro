package com.example.elosonoro.data.repository

import android.net.Uri
import android.util.Log
import com.example.elosonoro.data.local.dao.PlateDao
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.domain.repository.PlateRepository
import com.example.elosonoro.util.formatPlateName
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
        val formattedName = name.formatPlateName()
        if (formattedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("O nome da placa é obrigatório e não pode ser vazio."))
        }

        try {
            val plateId = id?.ifBlank { null }
            val existingPlate = if (plateId != null) {
                plateDao.getPlateByIdSync(plateId)
            } else null

            // Processamento e limpeza de imagem antiga na edição (Spec 2.4)
            val permanentImagePath = if (existingPlate != null) {
                if (imageUri != existingPlate.imageUri) {
                    // Mídia de imagem alterada ou removida: exclui o arquivo físico antigo
                    existingPlate.imageUri?.let { oldPath ->
                        fileManager.deleteFile(oldPath)
                        Log.d(TAG, "Imagem antiga removida fisicamente: $oldPath")
                    }
                    // Se nova imagem fornecida, copia para o armazenamento interno
                    processMediaImage(imageUri)
                } else {
                    // Imagem inalterada: preserva o caminho existente
                    existingPlate.imageUri
                }
            } else {
                // Nova placa sendo criada
                processMediaImage(imageUri)
            }

            // Processamento e limpeza de áudio antigo na edição (Spec 2.4)
            val permanentAudioPath = if (existingPlate != null) {
                if (audioUri != existingPlate.audioUri) {
                    // Mídia de áudio alterada ou removida: exclui o arquivo físico antigo
                    existingPlate.audioUri?.let { oldPath ->
                        fileManager.deleteFile(oldPath)
                        Log.d(TAG, "Áudio antigo removido fisicamente: $oldPath")
                    }
                    // Se novo áudio fornecido, copia para o armazenamento interno
                    processMediaAudio(audioUri)
                } else {
                    // Áudio inalterado: preserva o caminho existente
                    existingPlate.audioUri
                }
            } else {
                // Nova placa sendo criada
                processMediaAudio(audioUri)
            }

            val plateEntity = PlateEntity(
                id = plateId ?: UUID.randomUUID().toString(),
                name = formattedName,
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

            // Limpeza física dos arquivos de mídia associados (Spec 2.4)
            plate.imageUri?.let { path ->
                fileManager.deleteFile(path)
                Log.d(TAG, "Arquivo de imagem da placa excluído: $path")
            }
            plate.audioUri?.let { path ->
                fileManager.deleteFile(path)
                Log.d(TAG, "Arquivo de áudio da placa excluído: $path")
            }

            Log.d(TAG, "Placa excluída com sucesso: ${plate.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir placa: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePlateById(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val plate = plateDao.getPlateByIdSync(id)
            if (plate != null) {
                deletePlate(plate)
            } else {
                plateDao.deleteById(id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao excluir placa por id $id: ${e.message}", e)
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
