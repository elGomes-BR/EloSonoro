package com.example.elosonoro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gerenciamento de vínculos N:M entre Placas e Perfis.
 * Atende às especificações:
 * - Operações de vincular e desvincular Placas e Perfis
 * - Consultas diretas de associação
 */
@Dao
interface CrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: PlateProfileCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crossRefs: List<PlateProfileCrossRef>)

    @Delete
    suspend fun delete(crossRef: PlateProfileCrossRef)

    @Query("DELETE FROM plate_profile_cross_ref WHERE profileId = :profileId AND plateId = :plateId")
    suspend fun deleteRelation(profileId: String, plateId: String)

    @Query("DELETE FROM plate_profile_cross_ref WHERE profileId = :profileId")
    suspend fun deleteAllPlatesFromProfile(profileId: String)

    @Query("DELETE FROM plate_profile_cross_ref WHERE plateId = :plateId")
    suspend fun deleteAllProfilesFromPlate(plateId: String)

    @Query("""
        SELECT plates.* FROM plates
        INNER JOIN plate_profile_cross_ref ON plates.id = plate_profile_cross_ref.plateId
        WHERE plate_profile_cross_ref.profileId = :profileId
        ORDER BY plates.name ASC
    """)
    fun getPlatesForProfile(profileId: String): Flow<List<PlateEntity>>

    @Query("""
        SELECT plates.* FROM plates
        INNER JOIN plate_profile_cross_ref ON plates.id = plate_profile_cross_ref.plateId
        WHERE plate_profile_cross_ref.profileId = :profileId
        ORDER BY plates.name ASC
    """)
    suspend fun getPlatesForProfileSync(profileId: String): List<PlateEntity>

    @Query("""
        SELECT profiles.* FROM profiles
        INNER JOIN plate_profile_cross_ref ON profiles.id = plate_profile_cross_ref.profileId
        WHERE plate_profile_cross_ref.plateId = :plateId
        ORDER BY profiles.name ASC
    """)
    fun getProfilesForPlate(plateId: String): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM plate_profile_cross_ref WHERE profileId = :profileId")
    fun getCrossRefsForProfile(profileId: String): Flow<List<PlateProfileCrossRef>>

    @Query("SELECT * FROM plate_profile_cross_ref WHERE plateId = :plateId")
    fun getCrossRefsForPlate(plateId: String): Flow<List<PlateProfileCrossRef>>
}
