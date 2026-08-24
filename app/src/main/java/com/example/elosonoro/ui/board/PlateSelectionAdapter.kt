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
 */
class PlateSelectionAdapter(
    private val plates: List<PlateEntity>,
    initialSelectedIds: Set<String> = emptySet()
) : RecyclerView.Adapter<PlateSelectionAdapter.SelectionViewHolder>() {

    private val selectedIds = initialSelectedIds.toMutableSet()

    fun getSelectedPlateIds(): List<String> = selectedIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plate_selection, parent, false)
        return SelectionViewHolder(view)
    }

    override fun getItemCount(): Int = plates.size

    override fun onBindViewHolder(holder: SelectionViewHolder, position: Int) {
        val plate = plates[position]
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
