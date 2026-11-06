package com.example.elosonoro.ui.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para validar os critérios de aceite da Melhoria de Acessibilidade 13:
 * - Acesso protegido ao Menu no Modo de Uso via clique longo de 3 segundos (3.000 ms).
 * - Feedback orientativo e cancelamento imediato em toques rápidos no Modo de Uso.
 * - Resposta tátil e abertura do menu ao atingir os 3 segundos contínuos de sustentação.
 * - Acesso imediato no Modo Edição através de clique simples.
 */
class MenuAccessGatekeeperTest {

    private var isEditMode = false
    private var menuTriggeredCount = 0
    private var instructionFeedbackCount = 0
    private var hapticFeedbackCount = 0

    private lateinit var gatekeeper: MenuAccessGatekeeper

    @Before
    fun setUp() {
        isEditMode = false
        menuTriggeredCount = 0
        instructionFeedbackCount = 0
        hapticFeedbackCount = 0

        gatekeeper = MenuAccessGatekeeper(
            requiredDurationMs = 3000L,
            isEditModeProvider = { isEditMode },
            onMenuTriggered = { menuTriggeredCount++ },
            onInstructionFeedback = { instructionFeedbackCount++ },
            onHapticFeedback = { hapticFeedbackCount++ }
        )
    }

    @Test
    fun directClickInEditModeOpensMenuImmediately() {
        isEditMode = true

        gatekeeper.onDirectClick()

        assertEquals(1, menuTriggeredCount)
        assertEquals(0, instructionFeedbackCount)
        assertEquals(0, hapticFeedbackCount)
    }

    @Test
    fun directClickInUseModeShowsInstructionToastAndDoesNotOpenMenu() {
        isEditMode = false

        gatekeeper.onDirectClick()

        assertEquals(0, menuTriggeredCount)
        assertEquals(1, instructionFeedbackCount)
        assertEquals(0, hapticFeedbackCount)
    }

    @Test
    fun shortTouchInUseModeShowsInstructionAndDoesNotOpenMenu() {
        isEditMode = false
        val startTime = 1000L

        // Usuário toca na tela (ACTION_DOWN)
        val downConsumed = gatekeeper.onTouchDown(startTime)
        assertTrue("No Modo de Uso, o ACTION_DOWN deve ser consumido pelo gatekeeper", downConsumed)
        assertTrue(gatekeeper.isHolding)
        assertFalse(gatekeeper.hasTriggeredForCurrentPress)

        // Usuário solta o dedo após 500ms (ACTION_UP)
        val upResult = gatekeeper.onTouchUp(startTime + 500L)
        assertFalse("Não deve reportar conclusão de abertura no toque rápido", upResult)
        assertFalse(gatekeeper.isHolding)

        // Verifica que o menu NÃO foi aberto e o aviso foi emitido
        assertEquals(0, menuTriggeredCount)
        assertEquals(0, hapticFeedbackCount)
        assertEquals(1, instructionFeedbackCount)
    }

    @Test
    fun fullHold3SecondsInUseModeTriggersMenuAndHaptic() {
        isEditMode = false
        val startTime = 1000L

        // Inicia o toque
        gatekeeper.onTouchDown(startTime)
        assertTrue(gatekeeper.isHolding)

        // Temporizador atinge exatamente os 3 segundos (3.000 ms)
        val reached = gatekeeper.onDurationReached(startTime + 3000L)
        assertTrue("Ao atingir 3000ms contínuos, deve reportar acionamento", reached)
        assertTrue(gatekeeper.hasTriggeredForCurrentPress)
        assertFalse(gatekeeper.isHolding)

        // Disparou vibração e abriu menu
        assertEquals(1, hapticFeedbackCount)
        assertEquals(1, menuTriggeredCount)
        assertEquals(0, instructionFeedbackCount)

        // Quando o usuário finalmente solta o dedo (ACTION_UP)
        gatekeeper.onTouchUp(startTime + 3500L)
        // Não deve emitir aviso orientativo de erro
        assertEquals(0, instructionFeedbackCount)
        assertEquals(1, menuTriggeredCount)
    }

    @Test
    fun touchCancelInUseModeResetsStateWithoutTriggeringMenu() {
        isEditMode = false
        val startTime = 1000L

        gatekeeper.onTouchDown(startTime)
        assertTrue(gatekeeper.isHolding)

        // Dedo desliza para fora do botão ou sistema cancela (ACTION_CANCEL)
        gatekeeper.onTouchCancel()
        assertFalse(gatekeeper.isHolding)
        assertFalse(gatekeeper.hasTriggeredForCurrentPress)

        // Se o temporizador antigo tentar disparar após cancelamento, nada deve ocorrer
        val reached = gatekeeper.onDurationReached(startTime + 3000L)
        assertFalse(reached)
        assertEquals(0, menuTriggeredCount)
        assertEquals(0, instructionFeedbackCount)
        assertEquals(0, hapticFeedbackCount)
    }

    @Test
    fun touchDownInEditModeReturnsFalseToAllowDefaultHandling() {
        isEditMode = true

        val downConsumed = gatekeeper.onTouchDown(1000L)
        assertFalse("No Modo Edição, onTouchDown deve retornar false para permitir clique padrão", downConsumed)
        assertFalse(gatekeeper.isHolding)
    }

    @Test
    fun releaseBeforeDurationReachedPreventsMenuTriggerWhenTimerFires() {
        isEditMode = false
        val startTime = 1000L

        gatekeeper.onTouchDown(startTime)
        // Solta aos 1200ms
        gatekeeper.onTouchUp(startTime + 1200L)
        assertEquals(1, instructionFeedbackCount)

        // Temporizador dispara tardiamente aos 4000ms
        val reached = gatekeeper.onDurationReached(startTime + 3000L)
        assertFalse(reached)
        assertEquals(0, menuTriggeredCount)
        assertEquals(0, hapticFeedbackCount)
    }
}

