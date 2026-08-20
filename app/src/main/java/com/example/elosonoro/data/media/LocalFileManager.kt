package com.example.elosonoro.data.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

interface FileManager {
    fun getImagesDirectory(): File
    fun getAudioDirectory(): File
    fun saveImage(sourceUri: Uri): String
    fun saveImage(sourceUriString: String): String
    fun saveAudio(sourceUri: Uri): String
    fun saveAudio(sourceUriString: String): String
    fun saveImageStream(inputStream: InputStream, extension: String = "png"): String
    fun saveAudioStream(inputStream: InputStream, extension: String = "mp3"): String
    fun fileExists(pathOrUri: String?): Boolean
    fun getFile(pathOrUri: String?): File?
    fun deleteFile(pathOrUri: String?): Boolean
}

/**
 * Gerenciador de arquivos no sistema interno privado do aplicativo (Context.filesDir).
 * Conforme constitution.md e plan.md:
 * - Armazena fisicamente imagens e áudios no diretório interno.
 * - Renomeia arquivos com UUIDs únicos (img_<uuid>.<ext>, audio_<uuid>.<ext>) para evitar colisões.
 * - Retorna paths absolutos/URIs permanentes para registro no Room (sem BLOBs).
 */
class LocalFileManager(
    private val context: Context? = null,
    private val baseDir: File = context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "elosonoro_files")
) : FileManager {

    private val imagesDir: File by lazy {
        File(baseDir, "images").apply { if (!exists()) mkdirs() }
    }

    private val audioDir: File by lazy {
        File(baseDir, "audio").apply { if (!exists()) mkdirs() }
    }

    override fun getImagesDirectory(): File = imagesDir
    override fun getAudioDirectory(): File = audioDir

    /**
     * Copia uma imagem de uma URI de origem (galeria, câmera, etc.) para o armazenamento interno.
     * @return Caminho absoluto do arquivo local salvo.
     */
    override fun saveImage(sourceUri: Uri): String {
        val extension = getExtensionFromUri(sourceUri) ?: "png"
        val inputStream = context?.contentResolver?.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Não foi possível abrir o InputStream da URI: $sourceUri")
        return saveImageStream(inputStream, extension)
    }

    override fun saveImage(sourceUriString: String): String {
        return try {
            val uri = Uri.parse(sourceUriString)
            saveImage(uri)
        } catch (e: Exception) {
            val file = getFile(sourceUriString)
            if (file != null && file.exists()) {
                val extension = file.extension.ifBlank { "png" }
                saveImageStream(file.inputStream(), extension)
            } else {
                throw IllegalArgumentException("Arquivo/URI inválido: $sourceUriString", e)
            }
        }
    }

    /**
     * Copia um áudio de uma URI de origem (gravador, arquivo, etc.) para o armazenamento interno.
     * @return Caminho absoluto do arquivo local salvo.
     */
    override fun saveAudio(sourceUri: Uri): String {
        val extension = getExtensionFromUri(sourceUri) ?: "mp3"
        val inputStream = context?.contentResolver?.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Não foi possível abrir o InputStream da URI: $sourceUri")
        return saveAudioStream(inputStream, extension)
    }

    override fun saveAudio(sourceUriString: String): String {
        return try {
            val uri = Uri.parse(sourceUriString)
            saveAudio(uri)
        } catch (e: Exception) {
            val file = getFile(sourceUriString)
            if (file != null && file.exists()) {
                val extension = file.extension.ifBlank { "mp3" }
                saveAudioStream(file.inputStream(), extension)
            } else {
                throw IllegalArgumentException("Arquivo/URI inválido: $sourceUriString", e)
            }
        }
    }

    /**
     * Salva uma imagem a partir de um InputStream (ex: download da API ARASAAC).
     */
    override fun saveImageStream(inputStream: InputStream, extension: String): String {
        val fileName = "img_${UUID.randomUUID()}.$extension"
        val destinationFile = File(imagesDir, fileName)
        return copyStreamToFile(inputStream, destinationFile)
    }

    /**
     * Salva um áudio a partir de um InputStream.
     */
    override fun saveAudioStream(inputStream: InputStream, extension: String): String {
        val fileName = "audio_${UUID.randomUUID()}.$extension"
        val destinationFile = File(audioDir, fileName)
        return copyStreamToFile(inputStream, destinationFile)
    }

    /**
     * Verifica se o arquivo indicado pelo caminho/URI existe no sistema de arquivos.
     */
    override fun fileExists(pathOrUri: String?): Boolean {
        if (pathOrUri.isNullOrBlank()) return false
        val file = getFile(pathOrUri) ?: return false
        return file.exists() && file.length() > 0
    }

    /**
     * Obtém o objeto File a partir de uma String de path ou URI.
     */
    override fun getFile(pathOrUri: String?): File? {
        if (pathOrUri.isNullOrBlank()) return null
        val cleanPath = if (pathOrUri.startsWith("file://")) {
            pathOrUri.removePrefix("file://")
        } else {
            pathOrUri
        }
        return File(cleanPath)
    }

    /**
     * Exclui um arquivo físico do armazenamento interno.
     */
    override fun deleteFile(pathOrUri: String?): Boolean {
        val file = getFile(pathOrUri) ?: return false
        return if (file.exists()) file.delete() else false
    }

    private fun copyStreamToFile(inputStream: InputStream, destinationFile: File): String {
        inputStream.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        return destinationFile.absolutePath
    }

    private fun getExtensionFromUri(uri: Uri): String? {
        return try {
            val mimeType = context?.contentResolver?.getType(uri)
            if (mimeType != null) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            } else {
                uri.path?.substringAfterLast('.', "")?.ifBlank { null }
            }
        } catch (e: Exception) {
            null
        }
    }
}
