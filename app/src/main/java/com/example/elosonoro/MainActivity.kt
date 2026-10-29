package com.example.elosonoro

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.media.AudioRecorder
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.data.media.SoundPlaybackCoordinator
import com.example.elosonoro.util.formatPlateName
import com.example.elosonoro.ui.arasaac.ArasaacPictogramAdapter
import com.example.elosonoro.ui.arasaac.ArasaacSearchViewModel
import com.example.elosonoro.ui.board.BoardUiState
import com.example.elosonoro.ui.board.BoardViewModel
import com.example.elosonoro.ui.board.PlateAdapter
import com.example.elosonoro.ui.board.PlateSelectionAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val viewModel: BoardViewModel by viewModel()
    private val arasaacViewModel: ArasaacSearchViewModel by viewModel()
    private val soundCoordinator: SoundPlaybackCoordinator by inject()
    private val audioRecorder: AudioRecorder by inject()
    private val fileManager: FileManager by inject()

    private lateinit var recyclerPlates: RecyclerView
    private lateinit var plateAdapter: PlateAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var imgModeIndicator: ImageView
    private lateinit var btnOpenMenu: MaterialButton
    private lateinit var layoutSearchContainer: LinearLayout
    private lateinit var fabAddPlate: ExtendedFloatingActionButton
    private lateinit var edtSearch: TextInputEditText
    private lateinit var txtEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    // Image & Audio Picker Contracts
    private var pendingImagePreviewCallback: ((Uri) -> Unit)? = null
    private var pendingAudioPickerCallback: ((Uri) -> Unit)? = null
    private var pendingRecordAudioAction: (() -> Unit)? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingImagePreviewCallback?.invoke(it) }
    }

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingAudioPickerCallback?.invoke(it) }
    }

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingRecordAudioAction?.invoke()
        } else {
            Toast.makeText(this, "Permissão de microfone necessária para gravar áudio.", Toast.LENGTH_LONG).show()
        }
        pendingRecordAudioAction = null
    }

    private fun checkAndRequestAudioPermission(onGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            pendingRecordAudioAction = onGranted
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun initViews() {
        recyclerPlates = findViewById(R.id.recyclerPlates)
        imgModeIndicator = findViewById(R.id.imgModeIndicator)
        btnOpenMenu = findViewById(R.id.btnOpenMenu)
        layoutSearchContainer = findViewById(R.id.layoutSearchContainer)
        fabAddPlate = findViewById(R.id.fabAddPlate)
        edtSearch = findViewById(R.id.edtSearch)
        txtEmptyState = findViewById(R.id.txtEmptyState)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        plateAdapter = PlateAdapter(
            onPlateClick = { plate ->
                viewModel.onPlateClicked(plate)
            },
            onEditClick = { plate ->
                showPlateEditorDialog(plate)
            },
            onDeleteClick = { plate ->
                showDeletePlateConfirmation(plate)
            }
        )

        val initialSpan = viewModel.uiState.value.spanCount
        gridLayoutManager = GridLayoutManager(this, if (initialSpan in listOf(2, 4, 6)) initialSpan else 4)
        recyclerPlates.layoutManager = gridLayoutManager
        recyclerPlates.adapter = plateAdapter
    }

    private fun setupListeners() {
        // Botão para abrir o Menu Centralizado
        btnOpenMenu.setOnClickListener {
            showMainMenuDialog()
        }

        // Toque no ícone do modo também abre o Menu Centralizado
        imgModeIndicator.setOnClickListener {
            showMainMenuDialog()
        }

        // FAB Nova Placa (visível apenas no Modo Edição)
        fabAddPlate.setOnClickListener {
            showPlateEditorDialog(null)
        }

        // Busca em tempo real na prancha
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: BoardUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        txtEmptyState.visibility = if (state.plates.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE

        // Atualiza adapter de placas e modo
        plateAdapter.setEditMode(state.isEditMode)
        plateAdapter.submitList(state.plates)

        // Atualiza layout manager com o spanCount configurado (2, 4 ou 6)
        val validSpan = if (state.spanCount in listOf(2, 4, 6)) state.spanCount else 4
        if (gridLayoutManager.spanCount != validSpan) {
            gridLayoutManager.spanCount = validSpan
            recyclerPlates.adapter?.notifyDataSetChanged()
        }

        // Atualiza visual do Modo de Operação (Cadeado Fechado / Aberto) e Visibilidade Condicional da Busca
        if (state.isEditMode) {
            imgModeIndicator.setImageResource(R.drawable.ic_lock_open)
            imgModeIndicator.setColorFilter(ContextCompat.getColor(this, R.color.mode_edit_badge))
            imgModeIndicator.contentDescription = getString(R.string.mode_edit)
            layoutSearchContainer.visibility = View.VISIBLE
            fabAddPlate.visibility = View.VISIBLE
        } else {
            imgModeIndicator.setImageResource(R.drawable.ic_lock_closed)
            imgModeIndicator.setColorFilter(ContextCompat.getColor(this, R.color.mode_use_badge))
            imgModeIndicator.contentDescription = getString(R.string.mode_use)
            layoutSearchContainer.visibility = View.GONE
            fabAddPlate.visibility = View.GONE
        }

        // Mensagens ao usuário
        state.userMessage?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    /**
     * Exibe a janela modal de Menu Centralizado contendo:
     * 1. Seletor Unificado de Modo (Toggle com status e ícones de cadeado).
     * 2. Seletor de Densidade (2, 4 ou 6 colunas).
     * 3. Seletor de Categorias (Perfis) para filtragem da prancha.
     */
    private fun showMainMenuDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_main_menu)

        val cardModeToggle: com.google.android.material.card.MaterialCardView = dialog.findViewById(R.id.cardModeToggle) ?: return
        val imgModeLock: ImageView = dialog.findViewById(R.id.imgModeLock) ?: return
        val txtModeStatus: TextView = dialog.findViewById(R.id.txtModeStatus) ?: return
        val switchMode: com.google.android.material.materialswitch.MaterialSwitch = dialog.findViewById(R.id.switchMode) ?: return
        val toggleGroupSpan: com.google.android.material.button.MaterialButtonToggleGroup = dialog.findViewById(R.id.toggleGroupSpan) ?: return
        val chipGroupMenuCategories: ChipGroup = dialog.findViewById(R.id.chipGroupMenuCategories) ?: return
        val chipMenuAllPlates: Chip = dialog.findViewById(R.id.chipMenuAllPlates) ?: return
        val chipMenuAddCategory: Chip = dialog.findViewById(R.id.chipMenuAddCategory) ?: return
        val btnCloseMenu: MaterialButton = dialog.findViewById(R.id.btnCloseMenu) ?: return

        fun updateModeToggleVisual(isEdit: Boolean) {
            if (isEdit) {
                imgModeLock.setImageResource(R.drawable.ic_lock_open)
                imgModeLock.setColorFilter(ContextCompat.getColor(this, R.color.mode_edit_badge))
                txtModeStatus.text = getString(R.string.mode_edit)
                txtModeStatus.setTextColor(ContextCompat.getColor(this, R.color.mode_edit_badge))
                cardModeToggle.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mode_edit_badge_bg))
                cardModeToggle.strokeColor = ContextCompat.getColor(this, R.color.mode_edit_badge)
                switchMode.isChecked = true
            } else {
                imgModeLock.setImageResource(R.drawable.ic_lock_closed)
                imgModeLock.setColorFilter(ContextCompat.getColor(this, R.color.mode_use_badge))
                txtModeStatus.text = getString(R.string.mode_use)
                txtModeStatus.setTextColor(ContextCompat.getColor(this, R.color.mode_use_badge))
                cardModeToggle.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mode_use_badge_bg))
                cardModeToggle.strokeColor = ContextCompat.getColor(this, R.color.mode_use_badge)
                switchMode.isChecked = false
            }
        }

        val currentState = viewModel.uiState.value
        updateModeToggleVisual(currentState.isEditMode)

        cardModeToggle.setOnClickListener {
            val newMode = viewModel.toggleEditMode()
            updateModeToggleVisual(newMode)
            val msg = if (newMode) "Modo de Edição ativado" else "Modo de Uso ativado"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            renderMenuCategories(
                chipGroupMenuCategories,
                chipMenuAllPlates,
                chipMenuAddCategory,
                viewModel.uiState.value.profiles,
                viewModel.uiState.value.selectedProfileId,
                newMode,
                dialog
            )
        }

        // Configuração do Seletor de Densidade (2, 4 ou 6 colunas)
        when (currentState.spanCount) {
            2 -> toggleGroupSpan.check(R.id.btnSpan2)
            6 -> toggleGroupSpan.check(R.id.btnSpan6)
            else -> toggleGroupSpan.check(R.id.btnSpan4)
        }

        toggleGroupSpan.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newSpan = when (checkedId) {
                    R.id.btnSpan2 -> 2
                    R.id.btnSpan6 -> 6
                    else -> 4
                }
                viewModel.setSpanCount(newSpan)
            }
        }

        // Configuração do Seletor de Categorias
        renderMenuCategories(
            chipGroupMenuCategories,
            chipMenuAllPlates,
            chipMenuAddCategory,
            currentState.profiles,
            currentState.selectedProfileId,
            currentState.isEditMode,
            dialog
        )

        btnCloseMenu.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun renderMenuCategories(
        chipGroup: ChipGroup,
        chipAllPlates: Chip,
        chipAddCategory: Chip,
        profiles: List<ProfileEntity>,
        selectedProfileId: String?,
        isEditMode: Boolean,
        dialog: Dialog
    ) {
        val chipsToRemove = mutableListOf<View>()
        for (i in 0 until chipGroup.childCount) {
            val child = chipGroup.getChildAt(i)
            if (child.id != R.id.chipMenuAllPlates && child.id != R.id.chipMenuAddCategory) {
                chipsToRemove.add(child)
            }
        }
        chipsToRemove.forEach { chipGroup.removeView(it) }

        chipAllPlates.isChecked = (selectedProfileId == null)
        chipAllPlates.setOnClickListener {
            viewModel.selectProfile(null)
        }

        chipAddCategory.visibility = if (isEditMode) View.VISIBLE else View.GONE
        chipAddCategory.setOnClickListener {
            dialog.dismiss()
            showProfileEditorDialog(null)
        }

        val insertIndex = chipGroup.indexOfChild(chipAddCategory)

        profiles.forEach { profile ->
            val chip = Chip(this, null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
                text = profile.name
                isCheckable = true
                isChecked = (profile.id == selectedProfileId)
                setOnClickListener {
                    viewModel.selectProfile(profile.id)
                }

                if (isEditMode) {
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        dialog.dismiss()
                        showDeleteProfileConfirmation(profile)
                    }
                    setOnLongClickListener {
                        dialog.dismiss()
                        showProfileEditorDialog(profile)
                        true
                    }
                }
            }
            chipGroup.addView(chip, if (insertIndex >= 0) insertIndex else chipGroup.childCount)
        }
    }

    private fun showPlateEditorDialog(
        plateToEdit: PlateEntity?,
        defaultProfileId: String? = null,
        onSaved: ((PlateEntity) -> Unit)? = null
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_plate_editor)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val txtDialogTitle: TextView = dialog.findViewById(R.id.txtDialogTitle)
        val inputLayoutPlateName: TextInputLayout = dialog.findViewById(R.id.inputLayoutPlateName)
        val edtPlateName: TextInputEditText = dialog.findViewById(R.id.edtPlateName)
        val imgPreview: ImageView = dialog.findViewById(R.id.imgPreview)
        val btnSearchArasaac: MaterialButton = dialog.findViewById(R.id.btnSearchArasaac)
        val btnSelectImage: MaterialButton = dialog.findViewById(R.id.btnSelectImage)
        val btnClearImage: MaterialButton = dialog.findViewById(R.id.btnClearImage)
        val txtAudioStatus: TextView = dialog.findViewById(R.id.txtAudioStatus)
        val btnRecordAudio: MaterialButton = dialog.findViewById(R.id.btnRecordAudio)
        val btnSelectAudio: MaterialButton = dialog.findViewById(R.id.btnSelectAudio)
        val btnTestAudio: MaterialButton = dialog.findViewById(R.id.btnTestAudio)
        val btnClearAudio: MaterialButton = dialog.findViewById(R.id.btnClearAudio)
        val chipGroupCategories: ChipGroup = dialog.findViewById(R.id.chipGroupPlateCategories)
        val txtNoCategories: TextView = dialog.findViewById(R.id.txtNoCategoriesAvailable)
        val btnCancelPlate: MaterialButton = dialog.findViewById(R.id.btnCancelPlate)
        val btnSavePlate: MaterialButton = dialog.findViewById(R.id.btnSavePlate)

        var selectedImageUriString: String? = plateToEdit?.imageUri
        var selectedAudioUriString: String? = plateToEdit?.audioUri

        if (plateToEdit != null) {
            txtDialogTitle.text = "Editar Placa"
            edtPlateName.setText(plateToEdit.name)
            updateImagePreview(selectedImageUriString, imgPreview, btnClearImage)
            updateAudioStatus(selectedAudioUriString, txtAudioStatus, btnClearAudio)
        } else {
            txtDialogTitle.text = getString(R.string.add_plate)
            updateImagePreview(null, imgPreview, btnClearImage)
            updateAudioStatus(null, txtAudioStatus, btnClearAudio)
        }

        // Configuração dos chips de seleção de categorias (Plan 7.6.1 e Spec 2.4)
        val checkedCategoryIds = mutableSetOf<String>()
        if (plateToEdit == null) {
            val initialCategory = defaultProfileId ?: viewModel.uiState.value.selectedProfileId
            if (!initialCategory.isNullOrBlank()) {
                checkedCategoryIds.add(initialCategory)
            }
        }

        lifecycleScope.launch {
            if (plateToEdit != null) {
                try {
                    val linkedProfiles = viewModel.getProfilesForPlate(plateToEdit.id).first()
                    checkedCategoryIds.addAll(linkedProfiles.map { it.id })
                } catch (_: Exception) {}
            }
            viewModel.getAllProfiles().collectLatest { allProfiles ->
                chipGroupCategories.removeAllViews()
                if (allProfiles.isEmpty()) {
                    txtNoCategories.visibility = View.VISIBLE
                } else {
                    txtNoCategories.visibility = View.GONE
                    allProfiles.forEach { profile ->
                        val chip = Chip(this@MainActivity).apply {
                            text = profile.name
                            tag = profile.id
                            isCheckable = true
                            isChecked = checkedCategoryIds.contains(profile.id)
                            setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    checkedCategoryIds.add(profile.id)
                                } else {
                                    checkedCategoryIds.remove(profile.id)
                                }
                            }
                        }
                        chipGroupCategories.addView(chip)
                    }
                }
            }
        }

        // Gravar Áudio pelo Microfone (máx. 5s - Spec 1.1 e 2.4)
        btnRecordAudio.setOnClickListener {
            if (audioRecorder.isRecording()) {
                val recordedPath = audioRecorder.stopRecording()
                btnRecordAudio.text = "Gravar Voz (5s)"
                if (!recordedPath.isNullOrBlank()) {
                    selectedAudioUriString = recordedPath
                    updateAudioStatus(selectedAudioUriString, txtAudioStatus, btnClearAudio)
                }
            } else {
                checkAndRequestAudioPermission {
                    val audioDir = fileManager.getAudioDirectory()
                    if (!audioDir.exists()) audioDir.mkdirs()
                    val targetPath = File(audioDir, "audio_rec_${UUID.randomUUID()}.mp4").absolutePath

                    btnRecordAudio.text = "Gravando... (máx 5s)"
                    txtAudioStatus.text = "Gravando áudio do usuário (até 5 segundos)..."

                    val started = audioRecorder.startRecording(targetPath) {
                        // Callback quando o limite de 5s for atingido
                        runOnUiThread {
                            btnRecordAudio.text = "Gravar Voz (5s)"
                            selectedAudioUriString = targetPath
                            updateAudioStatus(selectedAudioUriString, txtAudioStatus, btnClearAudio)
                            Toast.makeText(this@MainActivity, "Limite de gravação de 5s atingido.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (!started) {
                        btnRecordAudio.text = "Gravar Voz (5s)"
                        txtAudioStatus.text = "Falha ao acessar microfone para gravação."
                    }
                }
            }
        }

        // Buscar na ARASAAC (Spec 2.2: Sugestão Automática de Nome e Pré-preenchimento da Busca)
        btnSearchArasaac.setOnClickListener {
            val currentName = edtPlateName.text?.toString()?.trim()
            showArasaacSearchDialog(initialQuery = currentName.takeIf { !it.isNullOrBlank() }) { downloadedLocalPath, suggestedName ->
                selectedImageUriString = downloadedLocalPath
                updateImagePreview(downloadedLocalPath, imgPreview, btnClearImage)
                val formattedSuggestedName = suggestedName.formatPlateName()
                if (formattedSuggestedName.isNotBlank()) {
                    edtPlateName.setText(formattedSuggestedName)
                    inputLayoutPlateName.error = null
                }
            }
        }

        // Escolher Imagem da Galeria
        btnSelectImage.setOnClickListener {
            pendingImagePreviewCallback = { uri ->
                selectedImageUriString = uri.toString()
                updateImagePreview(selectedImageUriString, imgPreview, btnClearImage)
            }
            selectImageLauncher.launch("image/*")
        }

        // Remover Imagem
        btnClearImage.setOnClickListener {
            selectedImageUriString = null
            updateImagePreview(null, imgPreview, btnClearImage)
        }

        // Escolher Áudio de Arquivo
        btnSelectAudio.setOnClickListener {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording()
                btnRecordAudio.text = "Gravar Voz (5s)"
            }
            pendingAudioPickerCallback = { uri ->
                selectedAudioUriString = uri.toString()
                updateAudioStatus(selectedAudioUriString, txtAudioStatus, btnClearAudio)
            }
            selectAudioLauncher.launch("audio/*")
        }

        // Ouvir Teste
        btnTestAudio.setOnClickListener {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording()
                btnRecordAudio.text = "Gravar Voz (5s)"
            }
            val name = edtPlateName.text?.toString() ?: ""
            lifecycleScope.launch {
                soundCoordinator.play(plateName = name.ifBlank { "Teste sonoro" }, audioUri = selectedAudioUriString)
            }
        }

        // Remover Áudio
        btnClearAudio.setOnClickListener {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording()
                btnRecordAudio.text = "Gravar Voz (5s)"
            }
            selectedAudioUriString = null
            updateAudioStatus(null, txtAudioStatus, btnClearAudio)
        }

        dialog.setOnDismissListener {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording()
            }
        }

        btnCancelPlate.setOnClickListener {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording()
            }
            dialog.dismiss()
        }

        // Botão Salvar (Validação Estrita e Persistência Transacional)
        btnSavePlate.setOnClickListener {
            if (audioRecorder.isRecording()) {
                val path = audioRecorder.stopRecording()
                if (!path.isNullOrBlank()) {
                    selectedAudioUriString = path
                }
            }
            val name = edtPlateName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                inputLayoutPlateName.error = getString(R.string.name_required)
                return@setOnClickListener
            }
            inputLayoutPlateName.error = null

            val selectedCategoryIds = checkedCategoryIds.toList()
            viewModel.savePlate(
                name = name,
                imageUri = selectedImageUriString,
                audioUri = selectedAudioUriString,
                id = plateToEdit?.id,
                categoryIds = selectedCategoryIds,
                onSuccess = onSaved
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateImagePreview(path: String?, imageView: ImageView, btnClear: MaterialButton) {
        if (path.isNullOrBlank()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            btnClear.visibility = View.GONE
            return
        }

        try {
            btnClear.visibility = View.VISIBLE
            val cleanPath = if (path.startsWith("file://")) path.removePrefix("file://") else path
            val file = File(cleanPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    return
                }
            }
            imageView.setImageURI(Uri.parse(path))
        } catch (e: Exception) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun updateAudioStatus(path: String?, txtStatus: TextView, btnClear: MaterialButton) {
        if (path.isNullOrBlank()) {
            txtStatus.text = "Status: Síntese de Voz (TTS pelo nome)"
            btnClear.visibility = View.GONE
        } else {
            txtStatus.text = "Status: Áudio gravado"
            btnClear.visibility = View.VISIBLE
        }
    }

    private fun showProfileEditorDialog(profileToEdit: ProfileEntity?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_profile_editor)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val txtProfileDialogTitle: TextView = dialog.findViewById(R.id.txtProfileDialogTitle)
        val inputLayoutProfileName: TextInputLayout = dialog.findViewById(R.id.inputLayoutProfileName)
        val edtProfileName: TextInputEditText = dialog.findViewById(R.id.edtProfileName)
        val edtSearchSelection: TextInputEditText = dialog.findViewById(R.id.edtSearchSelection)
        val btnQuickAddPlate: MaterialButton = dialog.findViewById(R.id.btnQuickAddPlate)
        val recyclerSelection: RecyclerView = dialog.findViewById(R.id.recyclerPlateSelection)
        val txtEmptyPlateSelection: TextView = dialog.findViewById(R.id.txtEmptyPlateSelection)
        val btnCancel: MaterialButton = dialog.findViewById(R.id.btnCancelProfile)
        val btnSave: MaterialButton = dialog.findViewById(R.id.btnSaveProfile)

        val selectionAdapter = PlateSelectionAdapter()
        recyclerSelection.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerSelection.adapter = selectionAdapter

        var initialLoadCompleted = false

        if (profileToEdit != null) {
            txtProfileDialogTitle.text = "Editar Categoria"
            edtProfileName.setText(profileToEdit.name)

            lifecycleScope.launch {
                // Carrega inicialmente as placas já vinculadas ao perfil
                viewModel.getPlatesForProfile(profileToEdit.id).collectLatest { linkedPlates ->
                    val linkedIds = linkedPlates.map { it.id }.toSet()
                    // Observa a totalidade de placas globais do sistema
                    viewModel.getAllPlates().collectLatest { allPlates ->
                        if (!initialLoadCompleted) {
                            selectionAdapter.setPlates(allPlates, linkedIds)
                            initialLoadCompleted = true
                        } else {
                            selectionAdapter.setPlates(allPlates, null)
                        }
                        txtEmptyPlateSelection.visibility = if (selectionAdapter.itemCount == 0) View.VISIBLE else View.GONE
                    }
                }
            }
        } else {
            txtProfileDialogTitle.text = getString(R.string.add_category)

            lifecycleScope.launch {
                viewModel.getAllPlates().collectLatest { allPlates ->
                    if (!initialLoadCompleted) {
                        selectionAdapter.setPlates(allPlates, emptySet())
                        initialLoadCompleted = true
                    } else {
                        selectionAdapter.setPlates(allPlates, null)
                    }
                    txtEmptyPlateSelection.visibility = if (selectionAdapter.itemCount == 0) View.VISIBLE else View.GONE
                }
            }
        }

        // Filtro de busca em tempo real
        edtSearchSelection.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                selectionAdapter.filter(s?.toString() ?: "")
                txtEmptyPlateSelection.visibility = if (selectionAdapter.itemCount == 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Atalho para criação rápida de placa no contexto da edição de perfil (Spec 2.5)
        btnQuickAddPlate.setOnClickListener {
            showPlateEditorDialog(null, defaultProfileId = profileToEdit?.id) { newlyCreatedPlate ->
                selectionAdapter.addPlate(newlyCreatedPlate, select = true)
                txtEmptyPlateSelection.visibility = if (selectionAdapter.itemCount == 0) View.VISIBLE else View.GONE
                recyclerSelection.scrollToPosition(0)
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = edtProfileName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                inputLayoutProfileName.error = "O nome da categoria é obrigatório."
                return@setOnClickListener
            }
            inputLayoutProfileName.error = null

            val selectedIds = selectionAdapter.getSelectedPlateIds()

            if (profileToEdit != null) {
                viewModel.updateProfile(
                    profile = profileToEdit.copy(name = name),
                    plateIds = selectedIds
                )
            } else {
                viewModel.createProfile(name = name, plateIds = selectedIds)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showArasaacSearchDialog(
        initialQuery: String? = null,
        onImageSelected: (savedLocalPath: String, suggestedName: String) -> Unit
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_arasaac_search)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val edtQuery: TextInputEditText = dialog.findViewById(R.id.edtArasaacQuery)
        val recycler: RecyclerView = dialog.findViewById(R.id.recyclerArasaac)
        val progress: ProgressBar = dialog.findViewById(R.id.progressArasaac)
        val txtMessage: TextView = dialog.findViewById(R.id.txtArasaacMessage)
        val btnClose: MaterialButton = dialog.findViewById(R.id.btnCloseArasaac)

        val adapter = ArasaacPictogramAdapter(lifecycleScope) { pictogram ->
            progress.visibility = View.VISIBLE
            txtMessage.text = "Baixando imagem..."
            txtMessage.visibility = View.VISIBLE

            arasaacViewModel.downloadAndSelectPictogram(pictogram) { savedLocalPath, suggestedName ->
                progress.visibility = View.GONE
                if (savedLocalPath != null) {
                    onImageSelected(savedLocalPath, suggestedName ?: pictogram.name)
                    dialog.dismiss()
                } else {
                    txtMessage.text = "Erro ao baixar imagem da ARASAAC"
                }
            }
        }

        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                arasaacViewModel.uiState.collectLatest { state ->
                    progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.pictograms)

                    if (state.errorMessage != null) {
                        txtMessage.text = state.errorMessage
                        txtMessage.visibility = View.VISIBLE
                    } else if (state.pictograms.isEmpty() && !state.isLoading) {
                        txtMessage.text = if (state.query.isBlank()) "Digite um termo acima para buscar pictogramas." else "Nenhum resultado."
                        txtMessage.visibility = View.VISIBLE
                    } else {
                        txtMessage.visibility = View.GONE
                    }
                }
            }
        }

        edtQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                arasaacViewModel.searchWithDebounce(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Pré-preenchimento da busca e disparo inicial caso fornecido (Spec 2.2)
        if (!initialQuery.isNullOrBlank()) {
            edtQuery.setText(initialQuery)
            edtQuery.setSelection(initialQuery.length)
            arasaacViewModel.searchImmediately(initialQuery)
        } else {
            edtQuery.setText("")
            arasaacViewModel.searchImmediately("")
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeletePlateConfirmation(plate: PlateEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Placa")
            .setMessage("Deseja realmente excluir a placa '${plate.name}' e seus arquivos de mídia?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deletePlate(plate)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteProfileConfirmation(profile: ProfileEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Excluir Categoria")
            .setMessage("Deseja realmente excluir a categoria '${profile.name}'? As placas associadas serão mantidas no aplicativo.")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteProfile(profile)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}