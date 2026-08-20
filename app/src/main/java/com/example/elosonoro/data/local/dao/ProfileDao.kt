package com.example.elosonoro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.model.ProfileWithPlates
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gerenciamento de Perfis/Categorias.
 * Atende às especificações:
 * - Inserção com OnConflictStrategy.ABORT para tratamento explícito de Unique Constraint
 * - Consultas relacionais completas (ProfileWithPlates)
 * - Observabilidade via Kotlin Flows
 */
@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getProfileById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileByIdSync(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE name = :name LIMIT 1")
    suspend fun getProfileByNameSync(name: String): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    fun getProfileWithPlates(profileId: String): Flow<ProfileWithPlates?>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfileWithPlatesSync(profileId: String): ProfileWithPlates?

    @Transaction
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfilesWithPlates(): Flow<List<ProfileWithPlates>>

    @Query("SELECT COUNT(*) FROM profiles")
    fun countProfiles(): Flow<Int>
}
