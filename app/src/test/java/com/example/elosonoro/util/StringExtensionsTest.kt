package com.example.elosonoro.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StringExtensionsTest {

    private val ptLocale = Locale.forLanguageTag("pt-BR")

    @Test
    fun testFormatPlateNameWithAllUppercase() {
        assertEquals("Água", "ÁGUA".formatPlateName(ptLocale))
        assertEquals("Bola azul", "BOLA AZUL".formatPlateName(ptLocale))
        assertEquals("Quero brincar", "QUERO BRINCAR".formatPlateName(ptLocale))
    }

    @Test
    fun testFormatPlateNameWithAllLowercase() {
        assertEquals("Água", "água".formatPlateName(ptLocale))
        assertEquals("Comida", "comida".formatPlateName(ptLocale))
        assertEquals("Quero ir embora", "quero ir embora".formatPlateName(ptLocale))
    }

    @Test
    fun testFormatPlateNameWithLeadingAndTrailingSpaces() {
        assertEquals("Maçã", "   MAÇÃ   ".formatPlateName(ptLocale))
        assertEquals("Parque", "  parque  ".formatPlateName(ptLocale))
    }

    @Test
    fun testFormatPlateNameWithSpecialCharactersAndAccents() {
        assertEquals("Ônibus", "ÔNIBUS".formatPlateName(ptLocale))
        assertEquals("Ímã", "ÍMÃ".formatPlateName(ptLocale))
        assertEquals("Atenção", "ATENÇÃO".formatPlateName(ptLocale))
    }

    @Test
    fun testFormatPlateNameBlankOrEmpty() {
        assertEquals("", "".formatPlateName(ptLocale))
        assertEquals("", "    ".formatPlateName(ptLocale))
    }
}
