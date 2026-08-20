package com.example.elosonoro.data.repository

import android.util.Log
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.data.remote.api.ArasaacApiService
import com.example.elosonoro.domain.model.ArasaacPictogram
import com.example.elosonoro.domain.repository.ArasaacRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Implementação do repositório ARASAAC.
 * Conforme plan.md (Seção 5) e spec.md (Seção 2.2):
 * - Consome a API ARASAAC via Retrofit
 * - Realiza download de stream de imagens e grava fisicamente no armazenamento interno via FileManager
 * - Retorna o caminho do arquivo físico local antes da persistência no banco
 */
class ArasaacRepositoryImpl(
    private val apiService: ArasaacApiService,
    private val fileManager: FileManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ArasaacRepository {

    companion object {
        private const val TAG = "ArasaacRepository"
        private const val IMAGE_BASE_URL = "https://static.arasaac.org/pictograms"
    }

    override suspend fun searchPictograms(
        term: String,
        locale: String
    ): Result<List<ArasaacPictogram>> = withContext(ioDispatcher) {
        if (term.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val responseList = apiService.searchPictograms(locale = locale, term = term.trim())
            val domainList = responseList.map { dto ->
                val primaryKeyword = dto.keywords?.firstOrNull()?.keyword ?: term.trim()
                val imageUrl = "$IMAGE_BASE_URL/${dto.id}/${dto.id}_500.png"
                ArasaacPictogram(
                    id = dto.id,
                    name = primaryKeyword,
                    imageUrl = imageUrl
                )
            }
            Result.success(domainList)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar pictogramas na ARASAAC para o termo '$term': ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadAndSavePictogram(
        pictogramId: Int,
        extension: String
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val response = apiService.downloadPictogram(pictogramId)
            if (!response.isSuccessful) {
                val errorMsg = "Falha no download do pictograma $pictogramId: HTTP ${response.code()}"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(IOException(errorMsg))
            }

            val responseBody = response.body()
                ?: return@withContext Result.failure(IOException("Corpo da resposta do pictograma $pictogramId está vazio"))

            val inputStream = responseBody.byteStream()
            val savedPath = fileManager.saveImageStream(inputStream, extension)
            Log.d(TAG, "Pictograma $pictogramId baixado e salvo com sucesso em: $savedPath")
            Result.success(savedPath)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar e salvar pictograma $pictogramId: ${e.message}", e)
            Result.failure(e)
        }
    }
}
