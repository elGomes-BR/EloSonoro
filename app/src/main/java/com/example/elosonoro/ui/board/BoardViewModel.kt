package com.example.elosonoro.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.preferences.UserPreferences
import com.example.elosonoro.domain.repository.PlateRepository
import com.example.elosonoro.domain.repository.ProfileRepository
import com.example.elosonoro.domain.usecase.CreateProfileUseCase
import com.example.elosonoro.domain.usecase.GetPlatesUseCase
import com.example.elosonoro.domain.usecase.GetProfilesWithPlatesUseCase
import com.example.elosonoro.domain.usecase.PlayPlateSoundUseCase
import com.example.elosonoro.domain.usecase.SavePlateUseCase
import com.example.elosonoro.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel da prancha principal de comunicação assistiva.
 * MVVM com StateFlow conforme plan.md e spec.md:
 * - Alternância e controle de Modo de Uso vs Modo de Edição (ModeState)
 * - Layout dinâmico com spanCount (2, 4, 8, 16)
 * - Filtro de busca inteligente (LIKE)
 * - Validação estrita contra submissões de campos vazios
 * - Disparo de áudio com baixa latência e telemetria
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModel(
    private val getPlatesUseCase: GetPlatesUseCase,
    private val getProfilesWithPlatesUseCase: GetProfilesWithPlatesUseCase,
    private val playPlateSoundUseCase: PlayPlateSoundUseCase,
    private val savePlateUseCase: SavePlateUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val plateRepository: PlateRepository,
    private val profileRepository: ProfileRepository,
    private val preferencesManager: UserPreferences
) : ViewModel() {

    private val _selectedProfileId = MutableStateFlow<String?>(preferencesManager.getSelectedProfileId())
    private val _searchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    // Flow observável de perfis com suas placas
    private val profilesWithPlatesFlow = getProfilesWithPlatesUseCase()

    // Flow observável de placas com suporte a busca e seleção de perfil
    private val platesFlow = combine(
        _selectedProfileId,
        _searchQuery
    ) { profileId, query ->
        Pair(profileId, query)
    }.flatMapLatest { (profileId, query) ->
        if (profileId == null) {
            // "Todas as placas"
            getPlatesUseCase(query)
        } else {
            // Placas da categoria selecionada
            profileRepository.getPlatesForProfile(profileId)
        }
    }

    val isEditModeFlow: StateFlow<Boolean> = preferencesManager.isEditModeFlow
    val spanCountFlow: StateFlow<Int> = preferencesManager.spanCountFlow

    val uiState: StateFlow<BoardUiState> = combine(
        profilesWithPlatesFlow,
        platesFlow,
        _selectedProfileId,
        _searchQuery,
        preferencesManager.isEditModeFlow,
        preferencesManager.spanCountFlow,
        _isLoading,
        _userMessage
    ) { params: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val profilesWithPlates = params[0] as List<com.example.elosonoro.data.local.model.ProfileWithPlates>
        @Suppress("UNCHECKED_CAST")
        val plates = params[1] as List<PlateEntity>
        val selectedProfileId = params[2] as String?
        val query = params[3] as String
        val isEditMode = params[4] as Boolean
        val spanCount = params[5] as Int
        val isLoading = params[6] as Boolean
        val userMessage = params[7] as String?

        // Se houver pesquisa ativa dentro de uma categoria específica, filtra por nome
        val filteredPlates = if (selectedProfileId != null && query.isNotBlank()) {
            plates.filter { it.name.contains(query.trim(), ignoreCase = true) }
        } else {
            plates
        }

        val profilesList = profilesWithPlates.map { it.profile }

        BoardUiState(
            profiles = profilesList,
            selectedProfileId = selectedProfileId,
            plates = filteredPlates,
            isEditMode = isEditMode,
            spanCount = spanCount,
            searchQuery = query,
            isLoading = isLoading,
            userMessage = userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BoardUiState(
            spanCount = preferencesManager.getSpanCount(),
            isEditMode = preferencesManager.isEditMode(),
            selectedProfileId = preferencesManager.getSelectedProfileId()
        )
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProfile(profileId: String?) {
        _selectedProfileId.value = profileId
        preferencesManager.setSelectedProfileId(profileId)
    }

    fun toggleEditMode(): Boolean {
        return preferencesManager.toggleEditMode()
    }

    fun setEditMode(isEditMode: Boolean) {
        preferencesManager.setEditMode(isEditMode)
    }

    fun setSpanCount(spanCount: Int) {
        preferencesManager.setSpanCount(spanCount)
    }

    fun onPlateClicked(plate: PlateEntity) {
        viewModelScope.launch {
            playPlateSoundUseCase(plate)
        }
    }

    fun getAllPlates(): Flow<List<PlateEntity>> = plateRepository.getAllPlates()

    fun getAllProfiles(): Flow<List<ProfileEntity>> = profileRepository.getAllProfiles()

    fun getProfilesForPlate(plateId: String): Flow<List<ProfileEntity>> =
        plateRepository.getProfilesForPlate(plateId)

    fun savePlate(
        name: String,
        imageUri: String? = null,
        audioUri: String? = null,
        id: String? = null,
        categoryIds: List<String>? = null,
        profileIdToLink: String? = null,
        onSuccess: ((PlateEntity) -> Unit)? = null
    ) {
        if (name.isBlank()) {
            _userMessage.value = "O nome da placa é obrigatório e não pode ser vazio."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val effectiveCategoryIds = when {
                categoryIds != null -> {
                    if (profileIdToLink != null && !categoryIds.contains(profileIdToLink)) {
                        categoryIds + profileIdToLink
                    } else {
                        categoryIds
                    }
                }
                profileIdToLink != null -> listOf(profileIdToLink)
                _selectedProfileId.value != null && id == null -> listOfNotNull(_selectedProfileId.value)
                else -> null
            }

            val result = savePlateUseCase(
                name = name,
                imageUri = imageUri,
                audioUri = audioUri,
                id = id,
                categoryIds = effectiveCategoryIds
            )

            result.onSuccess { savedPlate ->
                _userMessage.value = "Placa '${savedPlate.name}' salva com sucesso!"
                onSuccess?.invoke(savedPlate)
            }.onFailure { error ->
                _userMessage.value = error.message ?: "Erro ao salvar placa."
            }
            _isLoading.value = false
        }
    }

    fun deletePlate(plate: PlateEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = plateRepository.deletePlate(plate)
            result.onSuccess {
                _userMessage.value = "Placa '${plate.name}' excluída com sucesso."
            }.onFailure { error ->
                _userMessage.value = error.message ?: "Erro ao excluir placa."
            }
            _isLoading.value = false
        }
    }

    fun createProfile(name: String, plateIds: List<String> = emptyList()) {
        if (name.isBlank()) {
            _userMessage.value = "O nome da categoria é obrigatório e não pode ser vazio."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = createProfileUseCase(name = name, plateIds = plateIds)
            result.onSuccess { newProfile ->
                selectProfile(newProfile.id)
                _userMessage.value = "Categoria '${newProfile.name}' criada com sucesso!"
            }.onFailure { error ->
                _userMessage.value = error.message ?: "Erro ao criar categoria."
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(profile: ProfileEntity, plateIds: List<String>? = null) {
        if (profile.name.isBlank()) {
            _userMessage.value = "O nome da categoria é obrigatório e não pode ser vazio."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = updateProfileUseCase(profile = profile, plateIds = plateIds)
            result.onSuccess {
                _userMessage.value = "Categoria '${profile.name.trim()}' atualizada com sucesso!"
            }.onFailure { error ->
                _userMessage.value = error.message ?: "Erro ao atualizar categoria."
            }
            _isLoading.value = false
        }
    }

    fun getPlatesForProfile(profileId: String) = profileRepository.getPlatesForProfile(profileId)

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = profileRepository.deleteProfile(profile)
            result.onSuccess {
                if (_selectedProfileId.value == profile.id) {
                    selectProfile(null) // Volta para "Todas"
                }
                _userMessage.value = "Categoria '${profile.name}' excluída com sucesso."
            }.onFailure { error ->
                _userMessage.value = error.message ?: "Erro ao excluir categoria."
            }
            _isLoading.value = false
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
