package com.example.elosonoro.ui.board

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.entity.TelemetryEntity
import com.example.elosonoro.data.local.model.ProfileWithPlates
import com.example.elosonoro.data.preferences.UserPreferences
import com.example.elosonoro.domain.repository.PlateRepository
import com.example.elosonoro.domain.repository.ProfileRepository
import com.example.elosonoro.domain.repository.TelemetryRepository
import com.example.elosonoro.domain.usecase.CreateProfileUseCase
import com.example.elosonoro.domain.usecase.GetPlatesUseCase
import com.example.elosonoro.domain.usecase.GetProfilesWithPlatesUseCase
import com.example.elosonoro.domain.usecase.PlayPlateSoundUseCase
import com.example.elosonoro.domain.usecase.SavePlateUseCase
import com.example.elosonoro.domain.usecase.UpdateProfileUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitários para validar os critérios de aceitação da Refatoração 9:
 * - Acessibilidade motora: comutação estrita de spanCount entre 2, 4 ou 6 colunas.
 * - Redução de sobrecarga cognitiva: alternância de modo de uso vs edição.
 * - Pesquisa Inteligente: reatividade e filtragem de busca.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardAccessibilityRefactorTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakePlateRepo: FakePlateRepository
    private lateinit var fakeProfileRepo: FakeProfileRepository
    private lateinit var fakePreferences: FakePreferencesManager
    private lateinit var viewModel: BoardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakePlateRepo = FakePlateRepository()
        fakeProfileRepo = FakeProfileRepository()
        fakePreferences = FakePreferencesManager()

        viewModel = BoardViewModel(
            getPlatesUseCase = GetPlatesUseCase(fakePlateRepo),
            getProfilesWithPlatesUseCase = GetProfilesWithPlatesUseCase(fakeProfileRepo),
            playPlateSoundUseCase = PlayPlateSoundUseCase(
                playbackCoordinator = object : com.example.elosonoro.data.media.SoundPlaybackCoordinator(
                    localFileManager = object : com.example.elosonoro.data.media.FileManager {
                        override fun getImagesDirectory() = java.io.File("/tmp")
                        override fun getAudioDirectory() = java.io.File("/tmp")
                        override fun saveImage(sourceUri: android.net.Uri) = ""
                        override fun saveImage(sourceUriString: String) = ""
                        override fun saveAudio(sourceUri: android.net.Uri) = ""
                        override fun saveAudio(sourceUriString: String) = ""
                        override fun saveImageStream(inputStream: java.io.InputStream, extension: String) = ""
                        override fun saveAudioStream(inputStream: java.io.InputStream, extension: String) = ""
                        override fun fileExists(pathOrUri: String?) = true
                        override fun getFile(pathOrUri: String?) = null
                        override fun deleteFile(pathOrUri: String?) = true
                    },
                    audioPlayer = object : com.example.elosonoro.data.media.AudioPlayer {
                        override suspend fun playAudio(pathOrUri: String?) = true
                        override suspend fun playAudioAndWait(pathOrUri: String?) = true
                        override fun stopAll() {}
                        override fun release() {}
                    },
                    ttsPlayer = object : com.example.elosonoro.data.media.TtsPlayer {
                        override fun speak(text: String, queueMode: Int) = true
                        override suspend fun speakAndWait(text: String) = true
                        override fun stop() {}
                        override fun release() {}
                        override fun isReady() = true
                    }
                ) {},
                telemetryRepository = object : TelemetryRepository {
                    override suspend fun recordPlateUsage(plateId: String, timestamp: Long) = Result.success(Unit)
                    override fun getAllTelemetry(): Flow<List<TelemetryEntity>> = flowOf(emptyList())
                    override fun getTelemetryForPlate(plateId: String): Flow<List<TelemetryEntity>> = flowOf(emptyList())
                    override fun getPlateUsageCount(plateId: String): Flow<Int> = flowOf(0)
                    override suspend fun clearAllTelemetry() = Result.success(Unit)
                }
            ),
            savePlateUseCase = SavePlateUseCase(fakePlateRepo),
            createProfileUseCase = CreateProfileUseCase(fakeProfileRepo),
            updateProfileUseCase = UpdateProfileUseCase(fakeProfileRepo),
            plateRepository = fakePlateRepo,
            profileRepository = fakeProfileRepo,
            preferencesManager = fakePreferences
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDensitySelectorStrictlySupportsTwoFourAndSixColumns() = runTest(testDispatcher) {
        // Inicial padrão: 4 colunas
        assertEquals(4, viewModel.uiState.value.spanCount)

        // Comutação para 2 colunas (Acessibilidade Ampla)
        viewModel.setSpanCount(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.spanCount)

        // Comutação para 6 colunas (Densidade Máxima)
        viewModel.setSpanCount(6)
        advanceUntilIdle()
        assertEquals(6, viewModel.uiState.value.spanCount)

        // Comutação de volta para 4 colunas (Padrão)
        viewModel.setSpanCount(4)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.spanCount)
    }

    @Test
    fun testModeStateToggleTransitionsCorrectly() = runTest(testDispatcher) {
        // Inicial: Modo de Uso (isEditMode = false)
        assertFalse(viewModel.uiState.value.isEditMode)

        // Alterna para Modo de Edição
        val isEdit = viewModel.toggleEditMode()
        advanceUntilIdle()
        assertTrue(isEdit)
        assertTrue(viewModel.uiState.value.isEditMode)

        // Alterna de volta para Modo de Uso
        val isUse = viewModel.toggleEditMode()
        advanceUntilIdle()
        assertFalse(isUse)
        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun testSearchQueryFiltersPlatesInRealTime() = runTest(testDispatcher) {
        fakePlateRepo.plates.addAll(
            listOf(
                PlateEntity(id = "1", name = "Água"),
                PlateEntity(id = "2", name = "Biscoito"),
                PlateEntity(id = "3", name = "Banheiro")
            )
        )
        advanceUntilIdle()

        // Sem busca: 3 placas
        assertEquals(3, viewModel.uiState.value.plates.size)

        // Busca por "água"
        viewModel.setSearchQuery("água")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.plates.size)
        assertEquals("Água", viewModel.uiState.value.plates.first().name)

        // Busca por "b"
        viewModel.setSearchQuery("b")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.plates.size)

        // Limpa busca
        viewModel.setSearchQuery("")
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.plates.size)
    }

    private class FakePlateRepository : PlateRepository {
        val plates = mutableListOf<PlateEntity>()

        override fun getAllPlates(): Flow<List<PlateEntity>> = flowOf(plates)
        override fun getPlateById(id: String): Flow<PlateEntity?> = flowOf(plates.find { it.id == id })
        override suspend fun getPlateByIdSync(id: String): PlateEntity? = plates.find { it.id == id }
        override fun searchPlates(query: String): Flow<List<PlateEntity>> =
            flowOf(plates.filter { it.name.contains(query, ignoreCase = true) })

        override fun getProfilesForPlate(plateId: String): Flow<List<com.example.elosonoro.data.local.entity.ProfileEntity>> =
            flowOf(emptyList())

        override suspend fun savePlate(
            name: String,
            imageUri: String?,
            audioUri: String?,
            id: String?,
            categoryIds: List<String>?
        ): Result<PlateEntity> {
            val plate = PlateEntity(id = id ?: "pl-${plates.size + 1}", name = name, imageUri = imageUri, audioUri = audioUri)
            plates.add(plate)
            return Result.success(plate)
        }

        override suspend fun deletePlate(plate: PlateEntity): Result<Unit> {
            plates.removeIf { it.id == plate.id }
            return Result.success(Unit)
        }

        override suspend fun deletePlateById(id: String): Result<Unit> {
            plates.removeIf { it.id == id }
            return Result.success(Unit)
        }
    }

    private class FakeProfileRepository : ProfileRepository {
        val profiles = mutableListOf<ProfileEntity>()

        override fun getAllProfiles(): Flow<List<ProfileEntity>> = flowOf(profiles)
        override fun getProfileById(id: String): Flow<ProfileEntity?> = flowOf(profiles.find { it.id == id })
        override fun getAllProfilesWithPlates(): Flow<List<ProfileWithPlates>> = flowOf(profiles.map { ProfileWithPlates(it, emptyList()) })
        override fun getProfileWithPlates(profileId: String): Flow<ProfileWithPlates?> = flowOf(profiles.find { it.id == profileId }?.let { ProfileWithPlates(it, emptyList()) })
        override fun getPlatesForProfile(profileId: String): Flow<List<PlateEntity>> = flowOf(emptyList())

        override suspend fun createProfile(name: String, plateIds: List<String>): Result<ProfileEntity> {
            val prof = ProfileEntity(id = "prof-${profiles.size + 1}", name = name)
            profiles.add(prof)
            return Result.success(prof)
        }

        override suspend fun updateProfile(profile: ProfileEntity, plateIds: List<String>?): Result<Unit> = Result.success(Unit)
        override suspend fun deleteProfile(profile: ProfileEntity): Result<Unit> {
            profiles.removeIf { it.id == profile.id }
            return Result.success(Unit)
        }
        override suspend fun addPlateToProfile(profileId: String, plateId: String): Result<Unit> = Result.success(Unit)
        override suspend fun removePlateFromProfile(profileId: String, plateId: String): Result<Unit> = Result.success(Unit)
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

        private val _showSearchInUseModeFlow = MutableStateFlow(false)
        override val showSearchInUseModeFlow: StateFlow<Boolean> = _showSearchInUseModeFlow.asStateFlow()

        private val _showCategoriesInUseModeFlow = MutableStateFlow(false)
        override val showCategoriesInUseModeFlow: StateFlow<Boolean> = _showCategoriesInUseModeFlow.asStateFlow()

        override fun getSpanCount(): Int = spanCount
        override fun setSpanCount(spanCount: Int) {
            val validSpan = if (spanCount in listOf(2, 4, 6)) spanCount else 4
            this.spanCount = validSpan
            _spanCountFlow.value = validSpan
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
        override fun isShowSearchInUseMode(): Boolean = _showSearchInUseModeFlow.value
        override fun setShowSearchInUseMode(show: Boolean) { _showSearchInUseModeFlow.value = show }
        override fun isShowCategoriesInUseMode(): Boolean = _showCategoriesInUseModeFlow.value
        override fun setShowCategoriesInUseMode(show: Boolean) { _showCategoriesInUseModeFlow.value = show }
    }
}
