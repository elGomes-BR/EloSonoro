package com.example.elosonoro.ui.board

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.elosonoro.R
import com.example.elosonoro.data.local.entity.PlateEntity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File

/**
 * Adapter para renderização dos cartões de comunicação da prancha.
 * Suporta dinamismo entre Modo de Uso (ação de som ao toque) e Modo de Edição (ações de editar/excluir).
 */
class PlateAdapter(
    private val onPlateClick: (PlateEntity) -> Unit,
    private val onEditClick: (PlateEntity) -> Unit,
    private val onDeleteClick: (PlateEntity) -> Unit
) : ListAdapter<PlateEntity, PlateAdapter.PlateViewHolder>(PlateDiffCallback) {

    private var isEditMode: Boolean = false

    fun setEditMode(editMode: Boolean) {
        if (this.isEditMode != editMode) {
            this.isEditMode = editMode
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plate, parent, false)
        return PlateViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlateViewHolder, position: Int) {
        holder.bind(getItem(position), isEditMode, onPlateClick, onEditClick, onDeleteClick)
    }

    class PlateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardPlate: MaterialCardView = itemView.findViewById(R.id.cardPlate)
        private val imgPlate: ImageView = itemView.findViewById(R.id.imgPlate)
        private val txtPlateName: TextView = itemView.findViewById(R.id.txtPlateName)
        private val iconAudioIndicator: ImageView = itemView.findViewById(R.id.iconAudioIndicator)
        private val layoutEditActions: LinearLayout = itemView.findViewById(R.id.layoutEditActions)
        private val btnEditPlate: MaterialButton = itemView.findViewById(R.id.btnEditPlate)
        private val btnDeletePlate: MaterialButton = itemView.findViewById(R.id.btnDeletePlate)

        fun bind(
            plate: PlateEntity,
            isEditMode: Boolean,
            onPlateClick: (PlateEntity) -> Unit,
            onEditClick: (PlateEntity) -> Unit,
            onDeleteClick: (PlateEntity) -> Unit
        ) {
            txtPlateName.text = plate.name

            // Indicador de áudio gravado
            iconAudioIndicator.visibility = if (!plate.audioUri.isNullOrBlank()) View.VISIBLE else View.GONE

            // Carregamento de imagem local
            bindImage(plate.imageUri)

            // Controle do Modo de Edição vs Modo de Uso
            if (isEditMode) {
                layoutEditActions.visibility = View.VISIBLE
                btnEditPlate.setOnClickListener { onEditClick(plate) }
                btnDeletePlate.setOnClickListener { onDeleteClick(plate) }
            } else {
                layoutEditActions.visibility = View.GONE
            }

            // Clique principal no cartão: no Modo de Uso emite o som imediatamente (< 200ms)
            cardPlate.setOnClickListener {
                onPlateClick(plate)
            }
        }

        private fun bindImage(imageUriString: String?) {
            if (imageUriString.isNullOrBlank()) {
                imgPlate.setImageResource(android.R.drawable.ic_menu_gallery)
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
                        imgPlate.setImageBitmap(bitmap)
                        return
                    }
                }

                // Tenta carregar via URI
                val uri = Uri.parse(imageUriString)
                imgPlate.setImageURI(uri)
            } catch (e: Exception) {
                imgPlate.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    private object PlateDiffCallback : DiffUtil.ItemCallback<PlateEntity>() {
        override fun areItemsTheSame(oldItem: PlateEntity, newItem: PlateEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PlateEntity, newItem: PlateEntity): Boolean =
            oldItem == newItem
    }
}
