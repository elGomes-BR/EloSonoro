package com.example.elosonoro.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity

/**
 * Modelo relacional N:M representando uma Placa com todos os seus Perfis associados.
 */
data class PlateWithProfiles(
    @Embedded
    val plate: PlateEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlateProfileCrossRef::class,
            parentColumn = "plateId",
            entityColumn = "profileId"
        )
    )
    val profiles: List<ProfileEntity> = emptyList()
)
