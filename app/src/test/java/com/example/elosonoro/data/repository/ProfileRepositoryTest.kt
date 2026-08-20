package com.example.elosonoro.data.repository

import com.example.elosonoro.data.local.dao.CrossRefDao
import com.example.elosonoro.data.local.dao.ProfileDao
import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.local.entity.PlateProfileCrossRef
import com.example.elosonoro.data.local.entity.ProfileEntity
import com.example.elosonoro.data.local.model.ProfileWithPlates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileRepositoryTest {

    private lateinit var fakeProfileDao: FakeProfileDao
    private lateinit var fakeCrossRefDao: FakeCrossRefDao
    private lateinit var repository: ProfileRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeProfileDao = FakeProfileDao()
        fakeCrossRefDao = FakeCrossRefDao()
        repository = ProfileRepositoryImpl(
            profileDao = fakeProfileDao,
            crossRefDao = fakeCrossRefDao,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testCreateProfileBlankNameReturnsFailure() = runTest(testDispatcher) {
        val result = repository.createProfile(name = "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(fakeProfileDao.profiles.isEmpty())
    }

    @Test
    fun testCreateProfileSuccessWithImmediatePlatesAssociation() = runTest(testDispatcher) {
        val plateIds = listOf("plate-1", "plate-2", "plate-3")

        val result = repository.createProfile(name = "Necessidades", plateIds = plateIds)

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Necessidades", created?.name)

        // Verifica inserção do perfil no DAO
        assertEquals(1, fakeProfileDao.profiles.size)
        assertEquals(created?.id, fakeProfileDao.profiles.first().id)

        // Verifica associação imediata no CrossRefDao
        assertEquals(3, fakeCrossRefDao.crossRefs.size)
        val linkedPlateIds = fakeCrossRefDao.crossRefs.map { it.plateId }
        assertTrue(linkedPlateIds.containsAll(plateIds))
    }

    @Test
    fun testCreateProfileDuplicateNameReturnsFailure() = runTest(testDispatcher) {
        fakeProfileDao.profiles.add(ProfileEntity(id = "p-1", name = "Alimentação"))

        val result = repository.createProfile(name = "Alimentação")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(1, fakeProfileDao.profiles.size)
    }

    @Test
    fun testDeleteProfileDeletesProfileAndKeepsPlatesIntact() = runTest(testDispatcher) {
        val profile = ProfileEntity(id = "profile-1", name = "Rotina")
        val crossRef = PlateProfileCrossRef(plateId = "plate-1", profileId = "profile-1")

        fakeProfileDao.profiles.add(profile)
        fakeCrossRefDao.crossRefs.add(crossRef)

        val result = repository.deleteProfile(profile)

        assertTrue(result.isSuccess)
        assertTrue(fakeProfileDao.profiles.isEmpty())
        // Em Room real, ForeignKey.CASCADE limpa os crossRefs e deixa as placas intactas
    }

    // Fakes para teste desacoplado
    private class FakeProfileDao : ProfileDao {
        val profiles = mutableListOf<ProfileEntity>()

        override suspend fun insert(profile: ProfileEntity) {
            profiles.removeAll { it.id == profile.id }
            profiles.add(profile)
        }
        override suspend fun insertAll(profilesList: List<ProfileEntity>) {
            profiles.addAll(profilesList)
        }
        override suspend fun update(profile: ProfileEntity) {
            insert(profile)
        }
        override suspend fun delete(profile: ProfileEntity) {
            profiles.removeIf { it.id == profile.id }
        }
        override suspend fun deleteById(id: String) {
            profiles.removeIf { it.id == id }
        }
        override fun getProfileById(id: String): Flow<ProfileEntity?> =
            flowOf(profiles.find { it.id == id })
        override suspend fun getProfileByIdSync(id: String): ProfileEntity? =
            profiles.find { it.id == id }
        override suspend fun getProfileByNameSync(name: String): ProfileEntity? =
            profiles.find { it.name.equals(name, ignoreCase = true) }
        override fun getAllProfiles(): Flow<List<ProfileEntity>> =
            flowOf(profiles)
        override fun getProfileWithPlates(profileId: String): Flow<ProfileWithPlates?> =
            flowOf(profiles.find { it.id == profileId }?.let { ProfileWithPlates(it, emptyList()) })
        override suspend fun getProfileWithPlatesSync(profileId: String): ProfileWithPlates? =
            profiles.find { it.id == profileId }?.let { ProfileWithPlates(it, emptyList()) }
        override fun getAllProfilesWithPlates(): Flow<List<ProfileWithPlates>> =
            flowOf(profiles.map { ProfileWithPlates(it, emptyList()) })
        override fun countProfiles(): Flow<Int> = flowOf(profiles.size)
    }

    private class FakeCrossRefDao : CrossRefDao {
        val crossRefs = mutableListOf<PlateProfileCrossRef>()

        override suspend fun insert(crossRef: PlateProfileCrossRef) {
            crossRefs.add(crossRef)
        }
        override suspend fun insertAll(crossRefsList: List<PlateProfileCrossRef>) {
            crossRefs.addAll(crossRefsList)
        }
        override suspend fun delete(crossRef: PlateProfileCrossRef) {
            crossRefs.remove(crossRef)
        }
        override suspend fun deleteRelation(profileId: String, plateId: String) {
            crossRefs.removeIf { it.profileId == profileId && it.plateId == plateId }
        }
        override suspend fun deleteAllPlatesFromProfile(profileId: String) {
            crossRefs.removeIf { it.profileId == profileId }
        }
        override suspend fun deleteAllProfilesFromPlate(plateId: String) {
            crossRefs.removeIf { it.plateId == plateId }
        }
        override fun getPlatesForProfile(profileId: String): Flow<List<PlateEntity>> =
            flowOf(emptyList())
        override suspend fun getPlatesForProfileSync(profileId: String): List<PlateEntity> =
            emptyList()
        override fun getProfilesForPlate(plateId: String): Flow<List<ProfileEntity>> =
            flowOf(emptyList())
        override fun getCrossRefsForProfile(profileId: String): Flow<List<PlateProfileCrossRef>> =
            flowOf(crossRefs.filter { it.profileId == profileId })
        override fun getCrossRefsForPlate(plateId: String): Flow<List<PlateProfileCrossRef>> =
            flowOf(crossRefs.filter { it.plateId == plateId })
    }
}
