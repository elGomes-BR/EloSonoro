package com.example.elosonoro.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity

/**
 * Modelo relacional N:M representando um Perfil com todas as suas Placas associadas.
 */
data class ProfileWithPlates(
    @Embedded
    val profile: ProfileEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlateProfileCrossRef::class,
            parentColumn = "profileId",
            entityColumn = "plateId"
        )
    )
    val plates: List<PlateEntity> = emptyList()
)
