package com.example.elosonoro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidade de Perfil / Categoria (Profile).
 * Conforme plan.md:
 * - name deve ser único no banco de dados.
 */
@Entity(
    tableName = "profiles",
    indices = [Index(value = ["name"], unique = true)]
)
data class ProfileEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String
)
