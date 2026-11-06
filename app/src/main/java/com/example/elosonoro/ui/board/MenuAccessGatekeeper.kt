package com.example.elosonoro.ui.board

/**
 * Controlador de regras de acesso seguro ao Menu Centralizado.
 *
 * Conforme Spec 3.1 e 3.3 e Plan 7.3:
 * - Modo Edição (`isEditMode == true`): Acesso imediato via toque simples padrão.
 * - Modo de Uso (`isEditMode == false`): Exige clique longo sustentado de 3 segundos (3.000 ms).
 * - Toques rápidos no Modo de Uso cancelam o temporizador e exibem feedback orientativo ao cuidador.
 * - Conclusão dos 3 segundos emite resposta háptica e abre o Menu Centralizado.
 */
class MenuAccessGatekeeper(
    val requiredDurationMs: Long = 3000L,
    private val isEditModeProvider: () -> Boolean,
    private val onMenuTriggered: () -> Unit,
    private val onInstructionFeedback: () -> Unit,
    private val onHapticFeedback: () -> Unit = {}
) {
    var isHolding: Boolean = false
        private set

    var pressStartTimeMs: Long = 0L
        private set

    var hasTriggeredForCurrentPress: Boolean = false
        private set

    /**
     * Invocado quando o usuário inicia o toque (ACTION_DOWN).
     */
    fun onTouchDown(currentTimeMs: Long): Boolean {
        if (isEditModeProvider()) {
            return false
        }
        isHolding = true
        pressStartTimeMs = currentTimeMs
        hasTriggeredForCurrentPress = false
        return true
    }

    /**
     * Invocado quando o temporizador atinge a duração estipulada de 3 segundos contínuos.
     */
    fun onDurationReached(currentTimeMs: Long): Boolean {
        if (!isEditModeProvider() && isHolding && !hasTriggeredForCurrentPress) {
            if (currentTimeMs - pressStartTimeMs >= requiredDurationMs) {
                hasTriggeredForCurrentPress = true
                isHolding = false
                onHapticFeedback()
                onMenuTriggered()
                return true
            }
        }
        return false
    }

    /**
     * Invocado quando o usuário solta o toque (ACTION_UP).
     */
    fun onTouchUp(currentTimeMs: Long): Boolean {
        if (isEditModeProvider()) {
            onMenuTriggered()
            return true
        }

        if (isHolding && !hasTriggeredForCurrentPress) {
            isHolding = false
            onInstructionFeedback()
            return false
        }
        isHolding = false
        return false
    }

    /**
     * Invocado quando o gesto é cancelado ou sai dos limites visuais do botão (ACTION_CANCEL / MOVE out).
     */
    fun onTouchCancel() {
        isHolding = false
        hasTriggeredForCurrentPress = false
    }

    /**
     * Invocado em caso de acionamento direto ou via ferramentas de acessibilidade.
     */
    fun onDirectClick() {
        if (isEditModeProvider()) {
            onMenuTriggered()
        } else {
            onInstructionFeedback()
        }
    }
}

