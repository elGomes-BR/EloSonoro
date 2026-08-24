package com.example.elosonoro.ui.board

import android.net.Uri
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.entity.TelemetryEntity
import com.example.elosonoro.data.local.model.ProfileWithPlates
import com.example.elosonoro.data.media.AudioPlayer
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.data.media.SoundPlaybackCoordinator
import com.example.elosonoro.data.media.TtsPlayer
import com.example.elosonoro.data.preferences.UserPreferences
import com.example.elosonoro.domain.repository.PlateRepository
import com.example.elosonoro.domain.repository.ProfileRepository
import com.example.elosonoro.domain.repository.TelemetryRepository
import com.example.elosonoro.domain.usecase.CreateProfileUseCase
import com.example.elosonoro.domain.usecase.GetPlatesUseCase
import com.example.elosonoro.domain.usecase.GetProfilesWithPlatesUseCase
import com.example.elosonoro.domain.usecase.PlayPlateSoundUseCase
import com.example.elosonoro.domain.usecase.SavePlateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakePlateRepository: FakePlateRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeTelemetryRepository: FakeTelemetryRepository
    private lateinit var fakePreferencesManager: FakePreferencesManager
    private lateinit var fakePlaybackCoordinator: FakeSoundPlaybackCoordinator

    private lateinit var viewModel: BoardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakePlateRepository = FakePlateRepository()
        fakeProfileRepository = FakeProfileRepository()
        fakeTelemetryRepository = FakeTelemetryRepository()
        fakePreferencesManager = FakePreferencesManager()
        fakePlaybackCoordinator = FakeSoundPlaybackCoordinator()

        val getPlatesUseCase = GetPlatesUseCase(fakePlateRepository)
        val getProfilesWithPlatesUseCase = GetProfilesWithPlatesUseCase(fakeProfileRepository)
        val playPlateSoundUseCase = PlayPlateSoundUseCase(fakePlaybackCoordinator, fakeTelemetryRepository)
        val savePlateUseCase = SavePlateUseCase(fakePlateRepository)
        val createProfileUseCase = CreateProfileUseCase(fakeProfileRepository)
        val updateProfileUseCase = UpdateProfileUseCase(fakeProfileRepository)

        viewModel = BoardViewModel(
            getPlatesUseCase = getPlatesUseCase,
            getProfilesWithPlatesUseCase = getProfilesWithPlatesUseCase,
            playPlateSoundUseCase = playPlateSoundUseCase,
            savePlateUseCase = savePlateUseCase,
            createProfileUseCase = createProfileUseCase,
            updateProfileUseCase = updateProfileUseCase,
            plateRepository = fakePlateRepository,
            profileRepository = fakeProfileRepository,
            preferencesManager = fakePreferencesManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateDefaultsToModeUseAndFourColumns() {
        val state = viewModel.uiState.value
        assertEquals(4, state.spanCount)
        assertFalse(state.isEditMode)
        assertEquals(null, state.selectedProfileId)
    }

    @Test
    fun testToggleEditModeSwitchesState() = runTest(testDispatcher) {
        val isEdit = viewModel.toggleEditMode()
        advanceUntilIdle()
        assertTrue(isEdit)
        assertTrue(viewModel.uiState.value.isEditMode)

        val isUse = viewModel.toggleEditMode()
        advanceUntilIdle()
        assertFalse(isUse)
        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun testSetSpanCountUpdatesState() = runTest(testDispatcher) {
        viewModel.setSpanCount(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.spanCount)

        viewModel.setSpanCount(4)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.spanCount)
    }

    @Test
    fun testSavePlateBlankNameSetsValidationMessage() = runTest(testDispatcher) {
        viewModel.savePlate(name = "   ")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("obrigatório"))
    }

    @Test
    fun testSavePlateSuccessUpdatesPlateList() = runTest(testDispatcher) {
        viewModel.savePlate(name = "Água")
        advanceUntilIdle()

        assertEquals(1, fakePlateRepository.platesList.size)
        assertEquals("Água", fakePlateRepository.platesList.first().name)
    }

    @Test
    fun testCreateProfileBlankNameSetsValidationMessage() = runTest(testDispatcher) {
        viewModel.createProfile(name = "")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("obrigatório"))
    }

    @Test
    fun testUpdateProfileSuccessUpdatesUserMessage() = runTest(testDispatcher) {
        val profile = ProfileEntity(id = "p-1", name = "Alimentação")
        fakeProfileRepository.profilesList.add(profile)

        viewModel.updateProfile(profile = profile.copy(name = "Comidas"), plateIds = listOf("pl-1"))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("atualizada com sucesso"))
    }

    @Test
    fun testUpdateProfileBlankNameSetsValidationMessage() = runTest(testDispatcher) {
        val profile = ProfileEntity(id = "p-1", name = "Alimentação")
        viewModel.updateProfile(profile = profile.copy(name = "   "))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("obrigatório"))
    }

    @Test
    fun testOnPlateClickedExecutesPlaybackAndTelemetry() = runTest(testDispatcher) {
        val plate = PlateEntity(id = "p-1", name = "Quero água")
        viewModel.onPlateClicked(plate)
        advanceUntilIdle()

        assertEquals(listOf(plate), fakePlaybackCoordinator.playedPlates)
        assertEquals(listOf("p-1"), fakeTelemetryRepository.recordedPlateIds)
    }

    // Fakes para teste da UI
    private class FakePlateRepository : PlateRepository {
        val platesList = mutableListOf<PlateEntity>()

        override fun getAllPlates(): Flow<List<PlateEntity>> = flowOf(platesList)
        override fun getPlateById(id: String): Flow<PlateEntity?> = flowOf(platesList.find { it.id == id })
        override suspend fun getPlateByIdSync(id: String): PlateEntity? = platesList.find { it.id == id }
        override fun searchPlates(query: String): Flow<List<PlateEntity>> =
            flowOf(platesList.filter { it.name.contains(query, ignoreCase = true) })

        override suspend fun savePlate(
            name: String,
            imageUri: String?,
            audioUri: String?,
            id: String?
        ): Result<PlateEntity> {
            if (name.isBlank()) return Result.failure(IllegalArgumentException("Nome obrigatório"))
            val plate = PlateEntity(id = id ?: "id-${platesList.size + 1}", name = name, imageUri = imageUri, audioUri = audioUri)
            platesList.add(plate)
            return Result.success(plate)
        }

        override suspend fun deletePlate(plate: PlateEntity): Result<Unit> {
            platesList.removeIf { it.id == plate.id }
            return Result.success(Unit)
        }
    }

    private class FakeProfileRepository : ProfileRepository {
        val profilesList = mutableListOf<ProfileEntity>()

        override fun getAllProfiles(): Flow<List<ProfileEntity>> = flowOf(profilesList)
        override fun getProfileById(id: String): Flow<ProfileEntity?> = flowOf(profilesList.find { it.id == id })
        override fun getAllProfilesWithPlates(): Flow<List<ProfileWithPlates>> =
            flowOf(profilesList.map { ProfileWithPlates(it, emptyList()) })
        override fun getProfileWithPlates(profileId: String): Flow<ProfileWithPlates?> =
            flowOf(profilesList.find { it.id == profileId }?.let { ProfileWithPlates(it, emptyList()) })
        override fun getPlatesForProfile(profileId: String): Flow<List<PlateEntity>> = flowOf(emptyList())

        override suspend fun createProfile(name: String, plateIds: List<String>): Result<ProfileEntity> {
            if (name.isBlank()) return Result.failure(IllegalArgumentException("Nome obrigatório"))
            val profile = ProfileEntity(id = "prof-${profilesList.size + 1}", name = name)
            profilesList.add(profile)
            return Result.success(profile)
        }

        override suspend fun updateProfile(profile: ProfileEntity, plateIds: List<String>?): Result<Unit> = Result.success(Unit)
        override suspend fun deleteProfile(profile: ProfileEntity): Result<Unit> {
            profilesList.removeIf { it.id == profile.id }
            return Result.success(Unit)
        }
        override suspend fun addPlateToProfile(profileId: String, plateId: String): Result<Unit> = Result.success(Unit)
        override suspend fun removePlateFromProfile(profileId: String, plateId: String): Result<Unit> = Result.success(Unit)
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

    private class FakeSoundPlaybackCoordinator : SoundPlaybackCoordinator(
        localFileManager = FakeFileManager(),
        audioPlayer = FakeAudioPlayer(),
        ttsPlayer = FakeTtsPlayer()
    ) {
        val playedPlates = mutableListOf<PlateEntity>()

        override suspend fun playPlate(plate: PlateEntity): Boolean {
            playedPlates.add(plate)
            return true
        }
    }

    private class FakePreferencesManager : UserPreferences {
        private var spanCount = 4
        private var isEditMode = false
        private var selectedProfileId: String? = null

        private val _spanCountFlow = MutableStateFlow(spanCount)
        override val spanCountFlow: StateFlow<Int> = _spanCountFlow.asStateFlow()

        private val _isEditModeFlow = MutableStateFlow(isEditMode)
        override val isEditModeFlow: StateFlow<Boolean> = _isEditModeFlow.asStateFlow()

        private val _selectedProfileIdFlow = MutableStateFlow(selectedProfileId)
        override val selectedProfileIdFlow: StateFlow<String?> = _selectedProfileIdFlow.asStateFlow()

        override fun getSpanCount(): Int = spanCount
        override fun setSpanCount(spanCount: Int) {
            this.spanCount = spanCount
            _spanCountFlow.value = spanCount
        }
        override fun isEditMode(): Boolean = isEditMode
        override fun setEditMode(isEditMode: Boolean) {
            this.isEditMode = isEditMode
            _isEditModeFlow.value = isEditMode
        }
        override fun toggleEditMode(): Boolean {
            isEditMode = !isEditMode
            _isEditModeFlow.value = isEditMode
            return isEditMode
        }
        override fun getSelectedProfileId(): String? = selectedProfileId
        override fun setSelectedProfileId(profileId: String?) {
            this.selectedProfileId = profileId
            _selectedProfileIdFlow.value = profileId
        }
    }

    private class FakeFileManager : FileManager {
        override fun getImagesDirectory(): File = File("/tmp")
        override fun getAudioDirectory(): File = File("/tmp")
        override fun saveImage(sourceUri: Uri): String = ""
        override fun saveImage(sourceUriString: String): String = ""
        override fun saveAudio(sourceUri: Uri): String = ""
        override fun saveAudio(sourceUriString: String): String = ""
        override fun saveImageStream(inputStream: InputStream, extension: String): String = ""
        override fun saveAudioStream(inputStream: InputStream, extension: String): String = ""
        override fun fileExists(pathOrUri: String?): Boolean = true
        override fun getFile(pathOrUri: String?): File? = null
        override fun deleteFile(pathOrUri: String?): Boolean = true
    }

    private class FakeAudioPlayer : AudioPlayer {
        override suspend fun playAudio(pathOrUri: String?): Boolean = true
        override fun stopAll() {}
        override fun release() {}
    }

    private class FakeTtsPlayer : TtsPlayer {
        override fun speak(text: String, queueMode: Int): Boolean = true
        override fun stop() {}
        override fun release() {}
        override fun isReady(): Boolean = true
    }
}
