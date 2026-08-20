package com.example.elosonoro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.entity.TelemetryEntity

/**
 * Base Room Database para a aplicação EloSonoro.
 * Conforme constitution.md e plan.md:
 * - Persistência offline-first
 * - Armazenamento estrito de URIs locais de mídia (strings), proibido BLOBs.
 */
@Database(
    entities = [
        PlateEntity::class,
        ProfileEntity::class,
        PlateProfileCrossRef::class,
        TelemetryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EloSonoroDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "elo_sonoro.db"
    }
}
