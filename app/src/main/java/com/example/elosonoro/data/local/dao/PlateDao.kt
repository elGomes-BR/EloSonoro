package com.example.elosonoro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.elosonoro.data.local.entity.PlateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gerenciamento de Placas de comunicação.
 * Atende às especificações:
 * - CRUD completo de Placas
 * - Pesquisa inteligente por nome com filtro LIKE
 * - Observabilidade via Kotlin Flows
 */
@Dao
interface PlateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plate: PlateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plates: List<PlateEntity>)

    @Update
    suspend fun update(plate: PlateEntity)

    @Delete
    suspend fun delete(plate: PlateEntity)

    @Query("DELETE FROM plates WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM plates WHERE id = :id")
    fun getPlateById(id: String): Flow<PlateEntity?>

    @Query("SELECT * FROM plates WHERE id = :id")
    suspend fun getPlateByIdSync(id: String): PlateEntity?

    @Query("SELECT * FROM plates ORDER BY name ASC")
    fun getAllPlates(): Flow<List<PlateEntity>>

    @Query("SELECT * FROM plates WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPlatesByName(query: String): Flow<List<PlateEntity>>

    @Query("SELECT COUNT(*) FROM plates")
    fun countPlates(): Flow<Int>
}
