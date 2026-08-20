package com.example.elosonoro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Tabela associativa N:M entre Placas e Perfis.
 * Conforme spec.md:
 * - A exclusão de uma categoria desfaz o vínculo, mas não exclui as placas associadas a ela.
 */
@Entity(
    tableName = "plate_profile_cross_ref",
    primaryKeys = ["plateId", "profileId"],
    foreignKeys = [
        ForeignKey(
            entity = PlateEntity::class,
            parentColumns = ["id"],
            childColumns = ["plateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plateId"]),
        Index(value = ["profileId"])
    ]
)
data class PlateProfileCrossRef(
    val plateId: String,
    val profileId: String
)
