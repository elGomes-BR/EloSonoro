package com.example.elosonoro.data.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

interface TtsPlayer {
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH): Boolean
    fun stop()
    fun release()
    fun isReady(): Boolean
}

/**
 * Gerenciador de Text-To-Speech (TTS) nativo do Android.
 * Conforme constitution.md e plan.md:
 * - Inicialização assíncrona do sintetizador nativo
 * - Utilizado como sintetizador primário ou fallback para placas sem arquivo de áudio gravado.
 */
class TtsManager(
    context: Context
) : TextToSpeech.OnInitListener, TtsPlayer {

    companion object {
        private const val TAG = "TtsManager"
    }

    private var textToSpeech: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized: Boolean = false
    private var pendingTextToSpeak: String? = null

    init {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS iniciado: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS concluído: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Erro no TTS: $utteranceId")
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
     * Sintetiza em voz o texto fornecido.
     * @param text Texto a ser sintetizado (ex: nome da placa).
     * @param queueMode Modo de fila (padrão QUEUE_FLUSH para resposta imediata).
     * @return true se o comando foi aceito para síntese, false caso contrário.
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
     * Interrompe a fala atual.
     */
    override fun stop() {
        try {
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
