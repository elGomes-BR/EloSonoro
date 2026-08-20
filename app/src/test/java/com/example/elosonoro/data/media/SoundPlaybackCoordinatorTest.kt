package com.example.elosonoro.data.media

import android.net.Uri
import com.example.elosonoro.data.local.entity.PlateEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

class SoundPlaybackCoordinatorTest {

    private lateinit var fakeFileManager: FakeFileManager
    private lateinit var fakeAudioPlayer: FakeAudioPlayer
    private lateinit var fakeTtsPlayer: FakeTtsPlayer
    private lateinit var coordinator: SoundPlaybackCoordinator

    @Before
    fun setUp() {
        fakeFileManager = FakeFileManager()
        fakeAudioPlayer = FakeAudioPlayer()
        fakeTtsPlayer = FakeTtsPlayer()
        coordinator = SoundPlaybackCoordinator(fakeFileManager, fakeAudioPlayer, fakeTtsPlayer)
    }

    @Test
    fun testPlayWhenAudioExistsAndPlaysSuccessfully() = runTest {
        val audioPath = "file:///data/user/0/com.example.elosonoro/files/audio/audio_1.mp3"
        val plate = PlateEntity(name = "Água", audioUri = audioPath)

        fakeFileManager.existingFiles.add(audioPath)
        fakeAudioPlayer.nextPlayResult = true

        val result = coordinator.playPlate(plate)

        assertTrue(result)
        assertEquals(listOf(audioPath), fakeAudioPlayer.playedAudios)
        assertTrue(fakeTtsPlayer.spokenTexts.isEmpty())
    }

    @Test
    fun testPlayWhenAudioExistsButPlaybackFailsFallsBackToTts() = runTest {
        val audioPath = "file:///data/user/0/com.example.elosonoro/files/audio/corrupted.mp3"
        val plate = PlateEntity(name = "Comida", audioUri = audioPath)

        fakeFileManager.existingFiles.add(audioPath)
        fakeAudioPlayer.nextPlayResult = false
        fakeTtsPlayer.nextSpeakResult = true

        val result = coordinator.playPlate(plate)

        assertTrue(result)
        assertEquals(listOf(audioPath), fakeAudioPlayer.playedAudios)
        assertEquals(listOf("Comida"), fakeTtsPlayer.spokenTexts)
    }

    @Test
    fun testPlayWhenAudioUriIsNullDirectlyUsesTts() = runTest {
        val plate = PlateEntity(name = "Banheiro", audioUri = null)
        fakeTtsPlayer.nextSpeakResult = true

        val result = coordinator.playPlate(plate)

        assertTrue(result)
        assertTrue(fakeAudioPlayer.playedAudios.isEmpty())
        assertEquals(listOf("Banheiro"), fakeTtsPlayer.spokenTexts)
    }

    @Test
    fun testPlayWhenAudioFileDoesNotExistOnDiskDirectlyUsesTts() = runTest {
        val missingAudioPath = "file:///data/user/0/com.example.elosonoro/files/audio/missing.mp3"
        val plate = PlateEntity(name = "Dormir", audioUri = missingAudioPath)

        // missingAudioPath NÃO é adicionado a existingFiles
        fakeTtsPlayer.nextSpeakResult = true

        val result = coordinator.playPlate(plate)

        assertTrue(result)
        assertTrue(fakeAudioPlayer.playedAudios.isEmpty())
        assertEquals(listOf("Dormir"), fakeTtsPlayer.spokenTexts)
    }

    @Test
    fun testPlayWhenPlateNameIsBlankReturnsFalse() = runTest {
        val plate = PlateEntity(name = "", audioUri = null)

        val result = coordinator.playPlate(plate)

        assertFalse(result)
        assertTrue(fakeTtsPlayer.spokenTexts.isEmpty())
        assertTrue(fakeAudioPlayer.playedAudios.isEmpty())
    }

    // Fakes para teste desacoplado e de alta performance
    private class FakeFileManager : FileManager {
        val existingFiles = mutableSetOf<String>()

        override fun getImagesDirectory(): File = File("/tmp/images")
        override fun getAudioDirectory(): File = File("/tmp/audio")
        override fun saveImage(sourceUri: Uri): String = "img_saved.png"
        override fun saveImage(sourceUriString: String): String = "img_saved.png"
        override fun saveAudio(sourceUri: Uri): String = "audio_saved.mp3"
        override fun saveAudio(sourceUriString: String): String = "audio_saved.mp3"
        override fun saveImageStream(inputStream: InputStream, extension: String): String = "img_stream.$extension"
        override fun saveAudioStream(inputStream: InputStream, extension: String): String = "audio_stream.$extension"
        override fun fileExists(pathOrUri: String?): Boolean = pathOrUri in existingFiles
        override fun getFile(pathOrUri: String?): File? = pathOrUri?.let { File(it) }
        override fun deleteFile(pathOrUri: String?): Boolean = existingFiles.remove(pathOrUri)
    }

    private class FakeAudioPlayer : AudioPlayer {
        val playedAudios = mutableListOf<String?>()
        var nextPlayResult = true

        override suspend fun playAudio(pathOrUri: String?): Boolean {
            playedAudios.add(pathOrUri)
            return nextPlayResult
        }

        override fun stopAll() {}
        override fun release() {}
    }

    private class FakeTtsPlayer : TtsPlayer {
        val spokenTexts = mutableListOf<String>()
        var nextSpeakResult = true

        override fun speak(text: String, queueMode: Int): Boolean {
            spokenTexts.add(text)
            return nextSpeakResult
        }

        override fun stop() {}
        override fun release() {}
        override fun isReady(): Boolean = true
    }
}
