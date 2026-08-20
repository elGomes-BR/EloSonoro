package com.example.elosonoro.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface AudioPlayer {
    suspend fun playAudio(pathOrUri: String?): Boolean
    fun stopAll()
    fun release()
}

/**
 * Reprodutor de áudio de baixa latência baseado em SoundPool.
 * Conforme constitution.md e plan.md:
 * - Garante tempo de resposta < 200ms para reprodução de áudios curtos de comunicação.
 * - Mantém cache em memória dos áudios mais frequentes.
 */
class AudioPlayerManager(
    private val context: Context,
    private val maxStreams: Int = 4
) : AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayerManager"
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(maxStreams)
        .setAudioAttributes(audioAttributes)
        .build()

    // Cache de caminhos carregados: FilePath -> SoundId
    private val loadedSounds = ConcurrentHashMap<String, Int>()
    private val activeStreams = ConcurrentHashMap<Int, Int>() // streamId tracking

    init {
        setupLoadListener()
    }

    private var onLoadCompleteListener: ((soundPool: SoundPool, sampleId: Int, status: Int) -> Unit)? = null

    private fun setupLoadListener() {
        soundPool.setOnLoadCompleteListener { pool, sampleId, status ->
            onLoadCompleteListener?.invoke(pool, sampleId, status)
        }
    }

    /**
     * Reproduz um arquivo de áudio local de forma assíncrona com baixa latência.
     * @param pathOrUri Caminho físico do arquivo ou URI file://.
     * @return true se o áudio foi reproduzido com sucesso, false em caso de falha/arquivo inválido.
     */
    override suspend fun playAudio(pathOrUri: String?): Boolean {
        if (pathOrUri.isNullOrBlank()) return false

        val cleanPath = if (pathOrUri.startsWith("file://")) {
            pathOrUri.removePrefix("file://")
        } else {
            pathOrUri
        }

        val file = File(cleanPath)
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Arquivo de áudio não existe ou está vazio: $cleanPath")
            return false
        }

        return try {
            val cachedSoundId = loadedSounds[cleanPath]
            if (cachedSoundId != null && cachedSoundId > 0) {
                // Já carregado na memória: reprodução imediata (< 10ms)
                val streamId = soundPool.play(cachedSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                streamId > 0
            } else {
                // Carrega no SoundPool e toca assim que o sample estiver pronto
                loadAndPlay(cleanPath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao reproduzir áudio via SoundPool: ${e.message}", e)
            false
        }
    }

    private suspend fun loadAndPlay(filePath: String): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val soundId = soundPool.load(filePath, 1)
            if (soundId == 0) {
                if (continuation.isActive) continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            onLoadCompleteListener = { _, sampleId, status ->
                if (sampleId == soundId) {
                    if (status == 0) {
                        loadedSounds[filePath] = soundId
                        val streamId = soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                        if (continuation.isActive) continuation.resume(streamId > 0)
                    } else {
                        Log.e(TAG, "Falha ao decodificar áudio no SoundPool, sampleId: $sampleId, status: $status")
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção ao carregar áudio: ${e.message}", e)
            if (continuation.isActive) continuation.resume(false)
        }
    }

    /**
     * Interrompe todos os fluxos de som ativos no momento.
     */
    override fun stopAll() {
        soundPool.autoPause()
    }

    /**
     * Libera todos os recursos do SoundPool da memória.
     */
    override fun release() {
        try {
            loadedSounds.clear()
            soundPool.release()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar SoundPool: ${e.message}", e)
        }
    }
}
