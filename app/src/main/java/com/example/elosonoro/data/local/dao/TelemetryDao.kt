package com.example.elosonoro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.elosonoro.data.local.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gerenciamento do histórico silencioso de telemetria.
 * Conforme constitution.md e plan.md.
 */
@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(telemetry: TelemetryEntity): Long

    @Query("SELECT * FROM telemetry ORDER BY timestamp DESC")
    fun getAllTelemetry(): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry WHERE plateId = :plateId ORDER BY timestamp DESC")
    fun getTelemetryForPlate(plateId: String): Flow<List<TelemetryEntity>>

    @Query("SELECT COUNT(*) FROM telemetry WHERE plateId = :plateId")
    fun getCountForPlate(plateId: String): Flow<Int>

    @Query("DELETE FROM telemetry")
    suspend fun clearAll()
}
