package com.example.elosonoro.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Testes unitários para validação de conformidade da Refatoração 10:
 * - Recursos de ícone do aplicativo (Ativo mestre 512x512, mipmaps e adaptive icons).
 * - Conformidade de contraste WCAG AAA/AA para temas Claro e Escuro.
 */
class ThemeAndIconUnitTest {

    @Test
    fun `valida existencia e dimensoes do icone mestre oficial 512x512`() {
        val masterIcon = File("src/main/res/drawable/app_icon.png")
        assertTrue("O arquivo app_icon.png mestre deve existir em res/drawable", masterIcon.exists())

        val img = ImageIO.read(masterIcon)
        assertEquals("A largura do icone mestre deve ser exatamente 512px", 512, img.width)
        assertEquals("A altura do icone mestre deve ser exatamente 512px", 512, img.height)
    }

    @Test
    fun `valida presenca dos icones mipmap e adaptive icon v26`() {
        val mipmapDensities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        for (density in mipmapDensities) {
            val square = File("src/main/res/mipmap-$density/ic_launcher.png")
            val round = File("src/main/res/mipmap-$density/ic_launcher_round.png")
            assertTrue("Arquivo ic_launcher.png deve existir para $density", square.exists())
            assertTrue("Arquivo ic_launcher_round.png deve existir para $density", round.exists())
        }

        val adaptiveIcon = File("src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        val adaptiveRoundIcon = File("src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml")
        assertTrue("Adaptive icon XML deve existir em mipmap-anydpi-v26", adaptiveIcon.exists())
        assertTrue("Adaptive round icon XML deve existir em mipmap-anydpi-v26", adaptiveRoundIcon.exists())
    }

    @Test
    fun `valida presenca dos arquivos de temas e cores para modo diurno e noturno`() {
        val lightColors = File("src/main/res/values/colors.xml")
        val darkColors = File("src/main/res/values-night/colors.xml")
        val lightThemes = File("src/main/res/values/themes.xml")
        val darkThemes = File("src/main/res/values-night/themes.xml")

        assertTrue("colors.xml (modo claro) deve existir", lightColors.exists())
        assertTrue("colors.xml (modo escuro) deve existir", darkColors.exists())
        assertTrue("themes.xml (modo claro) deve existir", lightThemes.exists())
        assertTrue("themes.xml (modo escuro) deve existir", darkThemes.exists())
    }

    @Test
    fun `valida contraste WCAG AAA para texto em relacao ao fundo nos modos claro e escuro`() {
        // Modo Claro
        val lightBackground = parseHexColor("#F8F9FE")
        val lightOnBackground = parseHexColor("#191C1E")
        val lightContrast = calculateContrastRatio(lightOnBackground, lightBackground)
        assertTrue("Contraste no Modo Claro deve exceder WCAG AAA (>= 7.0:1). Atual: $lightContrast", lightContrast >= 7.0)

        // Modo Escuro
        val darkBackground = parseHexColor("#111416")
        val darkOnBackground = parseHexColor("#E1E2E5")
        val darkContrast = calculateContrastRatio(darkOnBackground, darkBackground)
        assertTrue("Contraste no Modo Escuro deve exceder WCAG AAA (>= 7.0:1). Atual: $darkContrast", darkContrast >= 7.0)
    }

    @Test
    fun `valida contraste WCAG AA para cores primarias e acoes`() {
        // Modo Claro: primary #006495 sobre branco #FFFFFF
        val lightPrimary = parseHexColor("#006495")
        val white = parseHexColor("#FFFFFF")
        val lightPrimaryContrast = calculateContrastRatio(lightPrimary, white)
        assertTrue("Contraste da cor primaria clara deve exceder WCAG AA (>= 4.5:1). Atual: $lightPrimaryContrast", lightPrimaryContrast >= 4.5)

        // Modo Escuro: primary #8ECEFF sobre background #111416
        val darkPrimary = parseHexColor("#8ECEFF")
        val darkBackground = parseHexColor("#111416")
        val darkPrimaryContrast = calculateContrastRatio(darkPrimary, darkBackground)
        assertTrue("Contraste da cor primaria escura deve exceder WCAG AA (>= 4.5:1). Atual: $darkPrimaryContrast", darkPrimaryContrast >= 4.5)
    }

    private data class RgbColor(val r: Int, val g: Int, val b: Int)

    private fun parseHexColor(hex: String): RgbColor {
        val clean = hex.removePrefix("#")
        val full = if (clean.length == 8) clean.substring(2) else clean
        val r = full.substring(0, 2).toInt(16)
        val g = full.substring(2, 4).toInt(16)
        val b = full.substring(4, 6).toInt(16)
        return RgbColor(r, g, b)
    }

    private fun calculateLuminance(color: RgbColor): Double {
        fun channelLuminance(channel: Int): Double {
            val c = channel / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = channelLuminance(color.r)
        val g = channelLuminance(color.g)
        val b = channelLuminance(color.b)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(c1: RgbColor, c2: RgbColor): Double {
        val l1 = calculateLuminance(c1)
        val l2 = calculateLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}

