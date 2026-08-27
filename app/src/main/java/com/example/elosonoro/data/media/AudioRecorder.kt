package com.example.elosonoro.data.media

/**
 * Contrato para gravação de áudio de voz do usuário.
 * Conforme spec.md (Seção 1.1 e 2.4) e plan.md (Seção 4):
 * - Limite máximo estrito de 5 segundos de gravação.
 */
interface AudioRecorder {

    /**
     * Duração máxima permitida para gravação de áudio em milissegundos (estritamente 5 segundos).
     */
    val maxDurationMs: Long
        get() = 5000L

    /**
     * Inicia a gravação de áudio salvando no caminho de arquivo fornecido.
     * @param outputFilePath Caminho absoluto do arquivo de saída.
     * @param onMaxDurationReached Callback opcional executado quando o limite de 5s for atingido.
     * @return true se a gravação foi iniciada com sucesso, false caso contrário.
     */
    fun startRecording(outputFilePath: String, onMaxDurationReached: (() -> Unit)? = null): Boolean

    /**
     * Interrompe a gravação em andamento e finaliza o arquivo de saída.
     * @return Caminho do arquivo gravado ou null se falhou/não estava gravando.
     */
    fun stopRecording(): String?

    /**
     * Retorna se há uma gravação ativa no momento.
     */
    fun isRecording(): Boolean

    /**
     * Libera recursos de hardware associados ao gravador.
     */
    fun release()
}
