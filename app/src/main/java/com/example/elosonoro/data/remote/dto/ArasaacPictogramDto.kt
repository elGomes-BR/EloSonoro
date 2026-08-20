package com.example.elosonoro.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para retorno de pictograma da API ARASAAC.
 */
data class ArasaacPictogramDto(
    @SerializedName("_id")
    val id: Int,

    @SerializedName("keywords")
    val keywords: List<ArasaacKeywordDto>? = null,

    @SerializedName("categories")
    val categories: List<String>? = null,

    @SerializedName("tags")
    val tags: List<String>? = null,

    @SerializedName("schematic")
    val schematic: Boolean? = null,

    @SerializedName("aac")
    val aac: Boolean? = null
)

/**
 * DTO para palavra-chave associada ao pictograma.
 */
data class ArasaacKeywordDto(
    @SerializedName("keyword")
    val keyword: String,

    @SerializedName("type")
    val type: Int? = null,

    @SerializedName("meaning")
    val meaning: String? = null,

    @SerializedName("plural")
    val plural: String? = null
)
