package com.example.elosonoro.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class LocalFileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File
    private lateinit var fileManager: LocalFileManager

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("app_files")
        fileManager = LocalFileManager(context = null, baseDir = baseDir)
    }

    @Test
    fun testSaveImageStreamCreatesUniqueFileInImagesDir() {
        val sampleData = "fake-image-bytes".toByteArray()
        val inputStream = ByteArrayInputStream(sampleData)

        val savedPath = fileManager.saveImageStream(inputStream, "png")
        assertNotNull(savedPath)
        assertTrue(savedPath.contains("img_"))
        assertTrue(savedPath.endsWith(".png"))

        val file = File(savedPath)
        assertTrue(file.exists())
        assertEquals(sampleData.size.toLong(), file.length())
        assertEquals("images", file.parentFile?.name)
    }

    @Test
    fun testSaveAudioStreamCreatesUniqueFileInAudioDir() {
        val sampleData = "fake-audio-bytes".toByteArray()
        val inputStream = ByteArrayInputStream(sampleData)

        val savedPath = fileManager.saveAudioStream(inputStream, "mp3")
        assertNotNull(savedPath)
        assertTrue(savedPath.contains("audio_"))
        assertTrue(savedPath.endsWith(".mp3"))

        val file = File(savedPath)
        assertTrue(file.exists())
        assertEquals(sampleData.size.toLong(), file.length())
        assertEquals("audio", file.parentFile?.name)
    }

    @Test
    fun testFileExistsAndGetFileWithAndWithoutFileScheme() {
        val sampleData = "test".toByteArray()
        val savedPath = fileManager.saveImageStream(ByteArrayInputStream(sampleData), "jpg")

        // Teste com path absoluto normal
        assertTrue(fileManager.fileExists(savedPath))
        assertNotNull(fileManager.getFile(savedPath))

        // Teste com URI no formato file://
        val uriPath = "file://$savedPath"
        assertTrue(fileManager.fileExists(uriPath))
        assertNotNull(fileManager.getFile(uriPath))

        // Arquivo inexistente
        assertFalse(fileManager.fileExists("/invalid/path/file.png"))
        assertFalse(fileManager.fileExists(null))
        assertFalse(fileManager.fileExists(""))
    }

    @Test
    fun testDeleteFileRemovesPhysicalFile() {
        val sampleData = "delete-me".toByteArray()
        val savedPath = fileManager.saveAudioStream(ByteArrayInputStream(sampleData), "wav")
        assertTrue(fileManager.fileExists(savedPath))

        val deleted = fileManager.deleteFile(savedPath)
        assertTrue(deleted)
        assertFalse(fileManager.fileExists(savedPath))
    }
}
