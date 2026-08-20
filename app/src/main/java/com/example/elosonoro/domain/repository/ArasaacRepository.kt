package com.example.elosonoro.domain.repository

import com.example.elosonoro.domain.model.ArasaacPictogram

/**
 * Contrato de repositório para operações com a API ARASAAC e persistência local de pictogramas.
 */
interface ArasaacRepository {

    /**
     * Pesquisa pictogramas disponíveis na ARASAAC por palavra-chave.
     * @param term Termo de busca (ex: "água", "comer", "escola").
     * @param locale Idioma da busca (padrão: "pt").
     * @return Result com a lista de pictogramas mapeados para exibição.
     */
    suspend fun searchPictograms(term: String, locale: String = "pt"): Result<List<ArasaacPictogram>>

    /**
     * Realiza o download assíncrono da imagem do pictograma, grava fisicamente no armazenamento
     * interno privado via FileManager e retorna o caminho físico do arquivo local.
     * @param pictogramId ID do pictograma na ARASAAC.
     * @param extension Extensão da imagem salva (padrão: "png").
     * @return Result com o caminho absoluto local (URI física) do arquivo salvo.
     */
    suspend fun downloadAndSavePictogram(pictogramId: Int, extension: String = "png"): Result<String>
}
