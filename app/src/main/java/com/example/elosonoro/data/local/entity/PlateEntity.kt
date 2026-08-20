package com.example.elosonoro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidade de Placa (Plate).
 * Conforme constitution.md e plan.md:
 * - Armazena estritamente URIs/caminhos de mídia como String.
 * - Armazenamento de BLOBs é terminantemente proibido.
 */
@Entity(tableName = "plates")
data class PlateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val imageUri: String? = null,
    val audioUri: String? = null
)
