package com.example.elosonoro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade de Telemetria para coleta silenciosa de histórico de uso.
 * Conforme constitution.md e plan.md.
 */
@Entity(
    tableName = "telemetry",
    foreignKeys = [
        ForeignKey(
            entity = PlateEntity::class,
            parentColumns = ["id"],
            childColumns = ["plateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plateId"])
    ]
)
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plateId: String,
    val timestamp: Long = System.currentTimeMillis()
)
