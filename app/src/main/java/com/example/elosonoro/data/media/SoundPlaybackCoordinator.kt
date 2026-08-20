package com.example.elosonoro.data.media

import android.util.Log
import com.example.elosonoro.data.local.entity.PlateEntity

/**
 * Coordenador responsável por executar o fluxo condicional de reprodução sonora.
 * Conforme spec.md (Seção 2.1) e plan.md (Seção 4):
 * 1. Verifica a existência de um arquivo de áudio local válido associado à placa.
 * 2. Se existir, reproduz o arquivo de áudio via AudioPlayerManager (SoundPool).
 * 3. Se não existir (ou a reprodução do arquivo falhar), aciona o Text-to-Speech (TTS) com o Nome da placa.
 */
class SoundPlaybackCoordinator(
    private val localFileManager: FileManager,
    private val audioPlayer: AudioPlayer,
    private val ttsPlayer: TtsPlayer
) {

    companion object {
        private const val TAG = "SoundPlaybackCoordinator"
    }

    /**
     * Executa a reprodução sonora para uma entidade Placa.
     */
    suspend fun playPlate(plate: PlateEntity): Boolean {
        return play(plateName = plate.name, audioUri = plate.audioUri)
    }

    /**
     * Executa a lógica condicional de reprodução sonora por nome e URI de áudio.
     * @param plateName Nome da placa (obrigatório para síntese TTS caso necessário).
     * @param audioUri URI ou caminho físico do áudio (opcional).
     * @return true se algum som foi reproduzido (áudio gravado ou TTS), false se ambos falharam.
     */
    suspend fun play(plateName: String, audioUri: String?): Boolean {
        if (plateName.isBlank()) {
            Log.w(TAG, "Tentativa de reprodução com nome de placa vazio.")
            return false
        }

        // 1. Verifica se o arquivo de áudio local existe e é válido
        val hasValidAudio = !audioUri.isNullOrBlank() && localFileManager.fileExists(audioUri)

        if (hasValidAudio) {
            try {
                // 2. Tenta reproduzir o áudio gravado via SoundPool (< 200ms)
                val audioPlayed = audioPlayer.playAudio(audioUri)
                if (audioPlayed) {
                    Log.d(TAG, "Áudio local reproduzido com sucesso para a placa: $plateName")
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

        // 3. Fallback: Aciona TTS nativo com o nome da placa
        return ttsPlayer.speak(plateName)
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
