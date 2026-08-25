package com.example.elosonoro.ui.board

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.elosonoro.R
import com.example.elosonoro.data.local.entity.PlateEntity
import com.google.android.material.checkbox.MaterialCheckBox
import java.io.File

/**
 * Adapter para seleção de múltiplas placas na criação/edição de perfis/categorias.
 * Conforme SDD/spec.md (Seção 2.5) e SDD/plan.md (Seção 7.4.2):
 * - Suporta busca/filtro em tempo real por nome sem perder a seleção de itens ocultados.
 * - Suporta adição dinâmica de placas ("Nova Placa") com seleção automática.
 * - Expõe a lista de IDs de placas selecionadas.
 */
class PlateSelectionAdapter(
    initialPlates: List<PlateEntity> = emptyList(),
    initialSelectedIds: Set<String> = emptySet()
) : RecyclerView.Adapter<PlateSelectionAdapter.SelectionViewHolder>() {

    private val originalPlates = initialPlates.toMutableList()
    private val displayedPlates = initialPlates.toMutableList()
    private val selectedIds = initialSelectedIds.toMutableSet()
    private var currentQuery: String = ""

    fun getSelectedPlateIds(): List<String> = selectedIds.toList()

    fun isSelected(plateId: String): Boolean = selectedIds.contains(plateId)

    fun getDisplayedPlates(): List<PlateEntity> = displayedPlates.toList()

    fun getOriginalPlates(): List<PlateEntity> = originalPlates.toList()

    /**
     * Define ou atualiza a listagem completa de placas disponíveis.
     * @param newPlates Nova lista total de placas.
     * @param newSelectedIds Conjunto opcional de IDs a serem selecionados. Se nulo, preserva as seleções atuais.
     */
    fun setPlates(newPlates: List<PlateEntity>, newSelectedIds: Set<String>? = null) {
        originalPlates.clear()
        originalPlates.addAll(newPlates)

        if (newSelectedIds != null) {
            selectedIds.clear()
            selectedIds.addAll(newSelectedIds)
        }

        applyCurrentFilter()
        notifySafely()
    }

    /**
     * Filtra as placas em tempo real pelo atributo 'name'.
     * Os itens marcados em [selectedIds] são estritamente preservados mesmo que ocultados pelo filtro.
     */
    fun filter(query: String) {
        currentQuery = query.trim()
        applyCurrentFilter()
        notifySafely()
    }

    /**
     * Insere uma nova placa (criada via atalho rápido) na listagem global e a marca como selecionada.
     * @param plate Placa criada e persistida.
     * @param select Se true, adiciona o ID da placa aos selecionados imediatamente.
     */
    fun addPlate(plate: PlateEntity, select: Boolean = true) {
        val existingIndex = originalPlates.indexOfFirst { it.id == plate.id }
        if (existingIndex >= 0) {
            originalPlates[existingIndex] = plate
        } else {
            // Insere no início para visibilidade imediata
            originalPlates.add(0, plate)
        }

        if (select) {
            selectedIds.add(plate.id)
        }

        applyCurrentFilter()
        notifySafely()
    }

    private fun applyCurrentFilter() {
        displayedPlates.clear()
        if (currentQuery.isEmpty()) {
            displayedPlates.addAll(originalPlates)
        } else {
            displayedPlates.addAll(
                originalPlates.filter { plate ->
                    plate.name.contains(currentQuery, ignoreCase = true)
                }
            )
        }
    }

    private fun notifySafely() {
        try {
            notifyDataSetChanged()
        } catch (_: Exception) {
            // Em testes unitários JVM puros, o Observable do RecyclerView pode ser nulo
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plate_selection, parent, false)
        return SelectionViewHolder(view)
    }

    override fun getItemCount(): Int = displayedPlates.size

    override fun onBindViewHolder(holder: SelectionViewHolder, position: Int) {
        val plate = displayedPlates[position]
        holder.bind(plate, selectedIds.contains(plate.id)) { isChecked ->
            if (isChecked) {
                selectedIds.add(plate.id)
            } else {
                selectedIds.remove(plate.id)
            }
        }
    }

    class SelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chkSelection: MaterialCheckBox = itemView.findViewById(R.id.chkPlateSelection)
        private val imgThumbnail: ImageView = itemView.findViewById(R.id.imgSelectionThumbnail)

        fun bind(plate: PlateEntity, isSelected: Boolean, onCheckedChange: (Boolean) -> Unit) {
            chkSelection.text = plate.name
            chkSelection.setOnCheckedChangeListener(null)
            chkSelection.isChecked = isSelected

            chkSelection.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(isChecked)
            }

            bindThumbnail(plate.imageUri)
        }

        private fun bindThumbnail(imageUriString: String?) {
            if (imageUriString.isNullOrBlank()) {
                imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
                return
            }

            try {
                val cleanPath = if (imageUriString.startsWith("file://")) {
                    imageUriString.removePrefix("file://")
                } else {
                    imageUriString
                }

                val file = File(cleanPath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        imgThumbnail.setImageBitmap(bitmap)
                        return
                    }
                }
                val uri = Uri.parse(imageUriString)
                imgThumbnail.setImageURI(uri)
            } catch (e: Exception) {
                imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}
