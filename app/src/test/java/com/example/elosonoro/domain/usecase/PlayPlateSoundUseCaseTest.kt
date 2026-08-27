package com.example.elosonoro.domain.usecase

import android.net.Uri
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.TelemetryEntity
import com.example.elosonoro.data.media.AudioPlayer
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.data.media.SoundPlaybackCoordinator
import com.example.elosonoro.data.media.TtsPlayer
import com.example.elosonoro.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

class PlayPlateSoundUseCaseTest {

    private lateinit var fakeFileManager: FakeFileManager
    private lateinit var fakeAudioPlayer: FakeAudioPlayer
    private lateinit var fakeTtsPlayer: FakeTtsPlayer
    private lateinit var playbackCoordinator: SoundPlaybackCoordinator
    private lateinit var fakeTelemetryRepository: FakeTelemetryRepository
    private lateinit var useCase: PlayPlateSoundUseCase

    @Before
    fun setUp() {
        fakeFileManager = FakeFileManager()
        fakeAudioPlayer = FakeAudioPlayer()
        fakeTtsPlayer = FakeTtsPlayer()
        playbackCoordinator = SoundPlaybackCoordinator(
            localFileManager = fakeFileManager,
            audioPlayer = fakeAudioPlayer,
            ttsPlayer = fakeTtsPlayer
        )
        fakeTelemetryRepository = FakeTelemetryRepository()
        useCase = PlayPlateSoundUseCase(
            playbackCoordinator = playbackCoordinator,
            telemetryRepository = fakeTelemetryRepository
        )
    }

    @Test
    fun testPlayPlateSoundSuccessPlaysSoundAndRecordsTelemetry() = runTest {
        val plate = PlateEntity(
            id = "plate-123",
            name = "Quero água",
            audioUri = null
        )
        fakeTtsPlayer.speakResult = true

        val result = useCase(plate)

        assertTrue(result)
        assertEquals(listOf("Quero água"), fakeTtsPlayer.spokenTexts)
        assertEquals(listOf("plate-123"), fakeTelemetryRepository.recordedPlateIds)
    }

    @Test
    fun testPlayPlateSoundBlankNameDoesNotPlayNorRecordTelemetry() = runTest {
        val plate = PlateEntity(
            id = "plate-456",
            name = "",
            audioUri = null
        )

        val result = useCase(plate)

        assertTrue(!result)
        assertTrue(fakeTelemetryRepository.recordedPlateIds.isEmpty())
    }

    private class FakeFileManager : FileManager {
        override fun getImagesDirectory(): File = File("/tmp/images")
        override fun getAudioDirectory(): File = File("/tmp/audio")
        override fun saveImage(sourceUri: Uri): String = ""
        override fun saveImage(sourceUriString: String): String = ""
        override fun saveAudio(sourceUri: Uri): String = ""
        override fun saveAudio(sourceUriString: String): String = ""
        override fun saveImageStream(inputStream: InputStream, extension: String): String = ""
        override fun saveAudioStream(inputStream: InputStream, extension: String): String = ""
        override fun fileExists(pathOrUri: String?): Boolean = false
        override fun getFile(pathOrUri: String?): File? = null
        override fun deleteFile(pathOrUri: String?): Boolean = true
    }

    private class FakeAudioPlayer : AudioPlayer {
        var playResult = true
        override suspend fun playAudio(pathOrUri: String?): Boolean = playResult
        override suspend fun playAudioAndWait(pathOrUri: String?): Boolean = playResult
        override fun stopAll() {}
        override fun release() {}
    }

    private class FakeTtsPlayer : TtsPlayer {
        val spokenTexts = mutableListOf<String>()
        var speakResult = true
        override fun speak(text: String, queueMode: Int): Boolean {
            spokenTexts.add(text)
            return speakResult
        }
        override suspend fun speakAndWait(text: String): Boolean {
            spokenTexts.add(text)
            return speakResult
        }
        override fun stop() {}
        override fun release() {}
        override fun isReady(): Boolean = true
    }

    private class FakeTelemetryRepository : TelemetryRepository {
        val recordedPlateIds = mutableListOf<String>()

        override suspend fun recordPlateUsage(plateId: String, timestamp: Long): Result<Unit> {
            recordedPlateIds.add(plateId)
            return Result.success(Unit)
        }
        override fun getAllTelemetry(): Flow<List<TelemetryEntity>> = flowOf(emptyList())
        override fun getTelemetryForPlate(plateId: String): Flow<List<TelemetryEntity>> = flowOf(emptyList())
        override fun getPlateUsageCount(plateId: String): Flow<Int> = flowOf(recordedPlateIds.count { it == plateId })
        override suspend fun clearAllTelemetry(): Result<Unit> {
            recordedPlateIds.clear()
            return Result.success(Unit)
        }
    }
}
