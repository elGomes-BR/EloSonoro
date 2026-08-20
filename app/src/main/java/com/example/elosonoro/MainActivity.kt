package com.example.elosonoro

import android.app.Dialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.example.elosonoro.data.media.SoundPlaybackCoordinator
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
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class MainActivity : AppCompatActivity() {

    private val viewModel: BoardViewModel by viewModel()
    private val arasaacViewModel: ArasaacSearchViewModel by viewModel()
    private val soundCoordinator: SoundPlaybackCoordinator by inject()

    private lateinit var recyclerPlates: RecyclerView
    private lateinit var plateAdapter: PlateAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipAllPlates: Chip
    private lateinit var chipAddCategory: Chip
    private lateinit var chipCurrentMode: Chip
    private lateinit var btnToggleMode: MaterialButton
    private lateinit var btnGridSpan: MaterialButton
    private lateinit var fabAddPlate: ExtendedFloatingActionButton
    private lateinit var edtSearch: TextInputEditText
    private lateinit var txtEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    // Image & Audio Picker Contracts
    private var pendingImagePreviewCallback: ((Uri) -> Unit)? = null
    private var pendingAudioPickerCallback: ((Uri) -> Unit)? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingImagePreviewCallback?.invoke(it) }
    }

    private val selectAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingAudioPickerCallback?.invoke(it) }
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
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        chipAllPlates = findViewById(R.id.chipAllPlates)
        chipAddCategory = findViewById(R.id.chipAddCategory)
        chipCurrentMode = findViewById(R.id.chipCurrentMode)
        btnToggleMode = findViewById(R.id.btnToggleMode)
        btnGridSpan = findViewById(R.id.btnGridSpan)
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

        gridLayoutManager = GridLayoutManager(this, 4)
        recyclerPlates.layoutManager = gridLayoutManager
        recyclerPlates.adapter = plateAdapter
    }

    private fun setupListeners() {
        // Toggle de Modo de Uso / Edição
        btnToggleMode.setOnClickListener {
            val isEdit = viewModel.toggleEditMode()
            val msg = if (isEdit) "Modo de Edição ativado" else "Modo de Uso ativado"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // Alteração de spanCount da grade
        btnGridSpan.setOnClickListener {
            showSpanCountDialog()
        }

        // Chip Todas as Placas
        chipAllPlates.setOnClickListener {
            viewModel.selectProfile(null)
        }

        // Chip Adicionar Categoria
        chipAddCategory.setOnClickListener {
            showCreateProfileDialog()
        }

        // FAB Nova Placa
        fabAddPlate.setOnClickListener {
            showPlateEditorDialog(null)
        }

        // Busca em tempo real
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

        // Atualiza layout manager com o spanCount configurado
        if (gridLayoutManager.spanCount != state.spanCount) {
            gridLayoutManager.spanCount = state.spanCount
            recyclerPlates.adapter?.notifyDataSetChanged()
        }

        // Atualiza visual do Modo de Operação
        if (state.isEditMode) {
            chipCurrentMode.text = getString(R.string.mode_edit)
            chipCurrentMode.setTextColor(ContextCompat.getColor(this, R.color.mode_edit_badge))
            chipCurrentMode.setChipBackgroundColorResource(R.color.mode_edit_badge_bg)
            chipAddCategory.visibility = View.VISIBLE
            fabAddPlate.visibility = View.VISIBLE
        } else {
            chipCurrentMode.text = getString(R.string.mode_use)
            chipCurrentMode.setTextColor(ContextCompat.getColor(this, R.color.mode_use_badge))
            chipCurrentMode.setChipBackgroundColorResource(R.color.mode_use_badge_bg)
            chipAddCategory.visibility = View.GONE
            fabAddPlate.visibility = View.GONE
        }

        // Renderiza Chips de Categorias
        renderCategoryChips(state.profiles, state.selectedProfileId, state.isEditMode)

        // Mensagens ao usuário
        state.userMessage?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    private fun renderCategoryChips(
        profiles: List<ProfileEntity>,
        selectedProfileId: String?,
        isEditMode: Boolean
    ) {
        // Mantém apenas o chipAllPlates e o chipAddCategory
        val chipsToRemove = mutableListOf<View>()
        for (i in 0 until chipGroupCategories.childCount) {
            val child = chipGroupCategories.getChildAt(i)
            if (child.id != R.id.chipAllPlates && child.id != R.id.chipAddCategory) {
                chipsToRemove.add(child)
            }
        }
        chipsToRemove.forEach { chipGroupCategories.removeView(it) }

        chipAllPlates.isChecked = (selectedProfileId == null)

        val insertIndex = chipGroupCategories.indexOfChild(chipAddCategory)

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
                        showDeleteProfileConfirmation(profile)
                    }
                }
            }
            chipGroupCategories.addView(chip, if (insertIndex >= 0) insertIndex else chipGroupCategories.childCount)
        }
    }

    private fun showSpanCountDialog() {
        val options = arrayOf("2 Colunas (Acessibilidade Ampla)", "4 Colunas (Padrão)")
        val spanValues = intArrayOf(2, 4)
        val currentSpan = viewModel.uiState.value.spanCount
        val checkedIndex = if (currentSpan == 2) 0 else 1

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.grid_columns)
            .setSingleChoiceItems(options, checkedIndex) { dialog, which ->
                viewModel.setSpanCount(spanValues[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPlateEditorDialog(plateToEdit: PlateEntity?) {
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
        val btnSelectAudio: MaterialButton = dialog.findViewById(R.id.btnSelectAudio)
        val btnTestAudio: MaterialButton = dialog.findViewById(R.id.btnTestAudio)
        val btnClearAudio: MaterialButton = dialog.findViewById(R.id.btnClearAudio)
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

        // Buscar na ARASAAC
        btnSearchArasaac.setOnClickListener {
            showArasaacSearchDialog { downloadedLocalPath ->
                selectedImageUriString = downloadedLocalPath
                updateImagePreview(downloadedLocalPath, imgPreview, btnClearImage)
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

        // Escolher Áudio
        btnSelectAudio.setOnClickListener {
            pendingAudioPickerCallback = { uri ->
                selectedAudioUriString = uri.toString()
                updateAudioStatus(selectedAudioUriString, txtAudioStatus, btnClearAudio)
            }
            selectAudioLauncher.launch("audio/*")
        }

        // Ouvir Teste
        btnTestAudio.setOnClickListener {
            val name = edtPlateName.text?.toString() ?: ""
            lifecycleScope.launch {
                soundCoordinator.play(plateName = name.ifBlank { "Teste sonoro" }, audioUri = selectedAudioUriString)
            }
        }

        // Remover Áudio
        btnClearAudio.setOnClickListener {
            selectedAudioUriString = null
            updateAudioStatus(null, txtAudioStatus, btnClearAudio)
        }

        btnCancelPlate.setOnClickListener {
            dialog.dismiss()
        }

        // Botão Salvar (Validação Estrita)
        btnSavePlate.setOnClickListener {
            val name = edtPlateName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                inputLayoutPlateName.error = getString(R.string.name_required)
                return@setOnClickListener
            }
            inputLayoutPlateName.error = null

            viewModel.savePlate(
                name = name,
                imageUri = selectedImageUriString,
                audioUri = selectedAudioUriString,
                id = plateToEdit?.id
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
            txtStatus.text = "Status: Arquivo de áudio gravado configurado"
            btnClear.visibility = View.VISIBLE
        }
    }

    private fun showCreateProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_profile_editor)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val inputLayoutProfileName: TextInputLayout = dialog.findViewById(R.id.inputLayoutProfileName)
        val edtProfileName: TextInputEditText = dialog.findViewById(R.id.edtProfileName)
        val recyclerSelection: RecyclerView = dialog.findViewById(R.id.recyclerPlateSelection)
        val btnCancel: MaterialButton = dialog.findViewById(R.id.btnCancelProfile)
        val btnSave: MaterialButton = dialog.findViewById(R.id.btnSaveProfile)

        val allPlates = viewModel.uiState.value.plates
        val selectionAdapter = PlateSelectionAdapter(allPlates)
        recyclerSelection.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerSelection.adapter = selectionAdapter

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = edtProfileName.text?.toString()?.trim() ?: ""
            if (name.isBlank()) {
                inputLayoutProfileName.error = "O nome da categoria é obrigatório."
                return@setOnClickListener
            }
            inputLayoutProfileName.error = null

            val selectedIds = selectionAdapter.getSelectedPlateIds()
            viewModel.createProfile(name = name, plateIds = selectedIds)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showArasaacSearchDialog(onImageSelected: (String) -> Unit) {
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

            arasaacViewModel.downloadAndSelectPictogram(pictogram.id) { savedLocalPath ->
                progress.visibility = View.GONE
                if (savedLocalPath != null) {
                    onImageSelected(savedLocalPath)
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