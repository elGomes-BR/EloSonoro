package com.example.elosonoro.data.remote.api

import com.example.elosonoro.data.remote.dto.ArasaacPictogramDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Interface Retrofit para consumo dos endpoints da API pública ARASAAC.
 * Base URL: https://api.arasaac.org/
 */
interface ArasaacApiService {

    /**
     * Busca pictogramas por termo de pesquisa no idioma selecionado (padrão pt).
     */
    @GET("v1/pictograms/{locale}/search/{term}")
    suspend fun searchPictograms(
        @Path("locale") locale: String = "pt",
        @Path("term") term: String
    ): List<ArasaacPictogramDto>

    /**
     * Download da imagem do pictograma por ID via streaming.
     */
    @Streaming
    @GET("v1/pictograms/{id}")
    suspend fun downloadPictogram(
        @Path("id") id: Int
    ): Response<ResponseBody>
}
