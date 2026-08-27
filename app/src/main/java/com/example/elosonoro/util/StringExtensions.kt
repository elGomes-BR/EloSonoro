package com.example.elosonoro.util

import java.util.Locale

/**
 * Normaliza e formata o nome de uma placa para garantir pronúncia fonética
 * natural pelo Text-to-Speech (TTS), evitando soletração causada por textos
 * totalmente em caixa alta (ex: "ÁGUA" -> "Água", "QUERO BRINCAR" -> "Quero brincar").
 */
fun String.formatPlateName(locale: Locale = Locale.getDefault()): String {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return ""
    val lower = trimmed.lowercase(locale)
    return lower.replaceFirstChar { firstChar ->
        if (firstChar.isLowerCase()) firstChar.titlecase(locale) else firstChar.toString()
    }
}
