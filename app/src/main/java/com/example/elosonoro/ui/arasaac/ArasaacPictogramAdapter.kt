package com.example.elosonoro.ui.arasaac

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.elosonoro.R
import com.example.elosonoro.domain.model.ArasaacPictogram
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Adapter para exibição de pictogramas da API ARASAAC em grade.
 */
class ArasaacPictogramAdapter(
    private val scope: CoroutineScope,
    private val onPictogramClick: (ArasaacPictogram) -> Unit
) : RecyclerView.Adapter<ArasaacPictogramAdapter.PictogramViewHolder>() {

    private var items: List<ArasaacPictogram> = emptyList()
    private val imageCache = ConcurrentHashMap<String, Bitmap>()

    fun submitList(list: List<ArasaacPictogram>) {
        this.items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictogramViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_arasaac_pictogram, parent, false)
        return PictogramViewHolder(view, scope, imageCache)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PictogramViewHolder, position: Int) {
        holder.bind(items[position], onPictogramClick)
    }

    class PictogramViewHolder(
        itemView: View,
        private val scope: CoroutineScope,
        private val imageCache: ConcurrentHashMap<String, Bitmap>
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardItem: MaterialCardView = itemView.findViewById(R.id.cardArasaacItem)
        private val imgPictogram: ImageView = itemView.findViewById(R.id.imgArasaacPictogram)
        private val txtName: TextView = itemView.findViewById(R.id.txtArasaacName)
        private var imageLoadingJob: Job? = null

        fun bind(item: ArasaacPictogram, onClick: (ArasaacPictogram) -> Unit) {
            txtName.text = item.name
            cardItem.setOnClickListener { onClick(item) }

            imageLoadingJob?.cancel()
            imgPictogram.setImageResource(android.R.drawable.ic_menu_gallery)

            val cached = imageCache[item.imageUrl]
            if (cached != null) {
                imgPictogram.setImageBitmap(cached)
            } else {
                imageLoadingJob = scope.launch(Dispatchers.IO) {
                    try {
                        val stream = URL(item.imageUrl).openStream()
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            imageCache[item.imageUrl] = bitmap
                            withContext(Dispatchers.Main) {
                                imgPictogram.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignora falha de thumbnail transitória
                    }
                }
            }
        }
    }
}
