package com.example.elosonoro.data.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

interface TtsPlayer {
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH): Boolean
    suspend fun speakAndWait(text: String): Boolean
    fun stop()
    fun release()
    fun isReady(): Boolean
}

/**
 * Gerenciador de Text-To-Speech (TTS) nativo do Android.
 * Conforme constitution.md e plan.md:
 * - Inicialização assíncrona do sintetizador nativo
 * - Utilizado como sintetizador primário ou fallback para placas sem arquivo de áudio gravado.
 * - Suporta suspensão com UtteranceProgressListener para fila sequencial de reprodução.
 */
class TtsManager(
    context: Context
) : TextToSpeech.OnInitListener, TtsPlayer {

    companion object {
        private const val TAG = "TtsManager"
        private const val TTS_TIMEOUT_MS = 8000L
    }

    private var textToSpeech: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized: Boolean = false
    private var pendingTextToSpeak: String? = null
    private val pendingUtteranceContinuations = ConcurrentHashMap<String, (Boolean) -> Unit>()

    init {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS iniciado: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS concluído com sucesso: $utteranceId")
                utteranceId?.let { id ->
                    pendingUtteranceContinuations.remove(id)?.invoke(true)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Erro no TTS: $utteranceId")
                utteranceId?.let { id ->
                    pendingUtteranceContinuations.remove(id)?.invoke(false)
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Erro no TTS (código $errorCode): $utteranceId")
                utteranceId?.let { id ->
                    pendingUtteranceContinuations.remove(id)?.invoke(false)
                }
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ptBrLocale = Locale.forLanguageTag("pt-BR")
            val result = textToSpeech?.setLanguage(ptBrLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Idioma pt_BR não disponível, utilizando idioma padrão do sistema")
                textToSpeech?.language = Locale.getDefault()
            }
            isInitialized = true
            Log.d(TAG, "TTS inicializado com sucesso.")

            pendingTextToSpeak?.let { text ->
                speak(text)
                pendingTextToSpeak = null
            }
        } else {
            Log.e(TAG, "Falha na inicialização do TTS: $status")
            isInitialized = false
        }
    }

    /**
     * Sintetiza em voz o texto fornecido sem bloquear.
     */
    override fun speak(text: String, queueMode: Int): Boolean {
        if (text.isBlank()) return false

        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS ainda não inicializado. Agendando reprodução de: $text")
            pendingTextToSpeak = text
            return false
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = textToSpeech?.speak(text, queueMode, params, utteranceId)
        return result == TextToSpeech.SUCCESS
    }

    /**
     * Sintetiza em voz o texto e suspende a execução até que o TTS finalize (onDone/onError).
     */
    override suspend fun speakAndWait(text: String): Boolean {
        if (text.isBlank()) return false

        // Aguarda inicialização se necessário (até 2s)
        var waitTime = 0L
        while ((!isInitialized || textToSpeech == null) && waitTime < 2000L) {
            delay(100L)
            waitTime += 100L
        }

        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS não inicializou a tempo para speakAndWait.")
            return false
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = withTimeoutOrNull(TTS_TIMEOUT_MS) {
            suspendCancellableCoroutine<Boolean> { continuation ->
                pendingUtteranceContinuations[utteranceId] = { success ->
                    if (continuation.isActive) {
                        continuation.resume(success)
                    }
                }

                continuation.invokeOnCancellation {
                    pendingUtteranceContinuations.remove(utteranceId)
                    stop()
                }

                val speakResult = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (speakResult != TextToSpeech.SUCCESS) {
                    pendingUtteranceContinuations.remove(utteranceId)
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        }

        return result ?: false
    }

    /**
     * Interrompe a fala atual.
     */
    override fun stop() {
        try {
            pendingUtteranceContinuations.clear()
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parar TTS: ${e.message}", e)
        }
    }

    /**
     * Libera o recurso TextToSpeech do sistema operacional.
     */
    override fun release() {
        try {
            pendingUtteranceContinuations.clear()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao finalizar TTS: ${e.message}", e)
        }
    }

    override fun isReady(): Boolean = isInitialized
}
