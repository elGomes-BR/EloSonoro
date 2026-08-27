package com.example.elosonoro.data.media

import android.util.Log
import com.example.elosonoro.data.local.entity.PlateEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordenador responsável por executar o fluxo condicional de reprodução sonora em fila sequencial (FIFO).
 * Conforme spec.md (Seção 2.1) e plan.md (Seção 4):
 * - A reprodução de várias placas em sequência aguarda o fim da reprodução anterior em uma estrutura de fila (FIFO).
 * - O item seguinte só é reproduzido após a conclusão do áudio ou da síntese TTS atual.
 * 1. Verifica a existência de um arquivo de áudio local válido associado à placa.
 * 2. Se existir, reproduz o arquivo de áudio via AudioPlayerManager aguardando o término.
 * 3. Se não existir (ou a reprodução do arquivo falhar), aciona o Text-to-Speech (TTS) nativo aguardando o término da fala.
 */
open class SoundPlaybackCoordinator(
    private val localFileManager: FileManager,
    private val audioPlayer: AudioPlayer,
    private val ttsPlayer: TtsPlayer
) {

    companion object {
        private const val TAG = "SoundPlaybackCoordinator"
    }

    // Mutex que gerencia a fila FIFO sequencial entre coroutines concorrentes
    private val playbackMutex = Mutex()

    /**
     * Executa a reprodução sonora para uma entidade Placa dentro da fila sequencial.
     */
    open suspend fun playPlate(plate: PlateEntity): Boolean {
        return play(plateName = plate.name, audioUri = plate.audioUri)
    }

    /**
     * Executa a reprodução ordenada de uma sequência de placas (ex: frase).
     */
    open suspend fun playSequential(plates: List<PlateEntity>): Boolean {
        var allSuccess = true
        for (plate in plates) {
            val success = playPlate(plate)
            if (!success) allSuccess = false
        }
        return allSuccess
    }

    /**
     * Executa a lógica condicional de reprodução sonora em estrutura de fila (FIFO).
     * @param plateName Nome da placa (obrigatório para síntese TTS caso necessário).
     * @param audioUri URI ou caminho físico do áudio (opcional).
     * @return true se algum som foi reproduzido (áudio gravado ou TTS), false se ambos falharam.
     */
    open suspend fun play(plateName: String, audioUri: String?): Boolean {
        if (plateName.isBlank()) {
            Log.w(TAG, "Tentativa de reprodução com nome de placa vazio.")
            return false
        }

        // Garante execução ordenada FIFO: se outro som estiver tocando, aguarda na fila
        return playbackMutex.withLock {
            executeSingleSound(plateName, audioUri)
        }
    }

    private suspend fun executeSingleSound(plateName: String, audioUri: String?): Boolean {
        // 1. Verifica se o arquivo de áudio local existe e é válido
        val hasValidAudio = !audioUri.isNullOrBlank() && localFileManager.fileExists(audioUri)

        if (hasValidAudio) {
            try {
                // 2. Reproduz o áudio gravado e aguarda seu término antes de liberar a fila
                val audioPlayed = audioPlayer.playAudioAndWait(audioUri)
                if (audioPlayed) {
                    Log.d(TAG, "Áudio local reproduzido sequencialmente para a placa: $plateName")
                    return true
                } else {
                    Log.w(TAG, "Falha ao reproduzir áudio local via SoundPool. Acionando fallback TTS para: $plateName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exceção na reprodução de áudio local: ${e.message}. Acionando fallback TTS.", e)
            }
        } else {
            Log.d(TAG, "Nenhum áudio gravado válido encontrado para '$plateName'. Acionando TTS nativo.")
        }

        // 3. Fallback: Aciona TTS nativo aguardando o evento de término (onDone)
        return ttsPlayer.speakAndWait(plateName)
    }

    /**
     * Interrompe qualquer som ou fala que esteja em execução.
     */
    fun stopAll() {
        audioPlayer.stopAll()
        ttsPlayer.stop()
    }

    /**
     * Libera recursos de hardware associados.
     */
    fun release() {
        audioPlayer.release()
        ttsPlayer.release()
    }
}
