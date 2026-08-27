package com.example.elosonoro.data.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Implementação do gravador de voz do usuário com limite estrito de 5 segundos.
 * Conforme spec.md (Seção 1.1 e 2.4) e plan.md (Seção 4).
 */
class AudioRecorderManager(
    private val context: Context
) : AudioRecorder {

    companion object {
        private const val TAG = "AudioRecorderManager"
        const val MAX_RECORDING_DURATION_MS = 5000L
    }

    override val maxDurationMs: Long = MAX_RECORDING_DURATION_MS

    private var mediaRecorder: MediaRecorder? = null
    private var isRecordingActive: Boolean = false
    private var currentOutputFilePath: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null
    private var maxDurationCallback: (() -> Unit)? = null

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    override fun startRecording(outputFilePath: String, onMaxDurationReached: (() -> Unit)?): Boolean {
        if (isRecordingActive) {
            Log.w(TAG, "Tentativa de iniciar gravação com outra já em andamento. Parando a anterior.")
            stopRecording()
        }

        val outputFile = File(outputFilePath)
        outputFile.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }

        maxDurationCallback = onMaxDurationReached
        currentOutputFilePath = outputFilePath

        return try {
            val recorder = createMediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(outputFilePath)
            recorder.setMaxDuration(maxDurationMs.toInt())

            recorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.d(TAG, "Limite máximo de duração de 5s atingido pelo MediaRecorder nativo.")
                    stopAndNotifyMaxDuration()
                }
            }

            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            isRecordingActive = true
            Log.d(TAG, "Gravação de áudio iniciada: $outputFilePath (limite de ${maxDurationMs}ms)")

            // Timer de segurança via Handler para garantir interrupção aos 5000ms
            autoStopRunnable = Runnable {
                if (isRecordingActive) {
                    Log.d(TAG, "Timer de segurança acionado para 5s.")
                    stopAndNotifyMaxDuration()
                }
            }
            autoStopRunnable?.let { mainHandler.postDelayed(it, maxDurationMs) }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar gravação de áudio: ${e.message}", e)
            cleanup()
            false
        }
    }

    private fun stopAndNotifyMaxDuration() {
        val path = stopRecording()
        if (path != null) {
            maxDurationCallback?.invoke()
        }
    }

    override fun stopRecording(): String? {
        if (!isRecordingActive) {
            return currentOutputFilePath
        }

        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = null

        val path = currentOutputFilePath
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Falha ao parar MediaRecorder (possível parada imediata após início): ${e.message}")
                }
                reset()
                release()
            }
            mediaRecorder = null
            isRecordingActive = false
            Log.d(TAG, "Gravação de áudio concluída com sucesso: $path")
            path
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao finalizar gravação de áudio: ${e.message}", e)
            cleanup()
            null
        }
    }

    override fun isRecording(): Boolean = isRecordingActive

    private fun cleanup() {
        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = null
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Erro na limpeza do MediaRecorder: ${e.message}")
        }
        mediaRecorder = null
        isRecordingActive = false
        maxDurationCallback = null
    }

    override fun release() {
        cleanup()
    }
}
