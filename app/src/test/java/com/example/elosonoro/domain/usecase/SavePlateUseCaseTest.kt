package com.example.elosonoro.domain.usecase

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.domain.repository.PlateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SavePlateUseCaseTest {

    private lateinit var fakeRepository: FakePlateRepository
    private lateinit var savePlateUseCase: SavePlateUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeRepository = FakePlateRepository()
        savePlateUseCase = SavePlateUseCase(plateRepository = fakeRepository)
    }

    @Test
    fun testSavePlateSuccessWithCategoryIds() = runTest(testDispatcher) {
        val result = savePlateUseCase(
            name = "água gelada",
            imageUri = "content://image/1",
            audioUri = "content://audio/1",
            categoryIds = listOf("cat-1", "cat-2")
        )

        assertTrue(result.isSuccess)
        val saved = result.getOrNull()
        assertNotNull(saved)
        assertEquals("Água gelada", saved?.name)
        assertEquals(listOf("cat-1", "cat-2"), fakeRepository.lastCategoryIds)
    }

    @Test
    fun testSavePlateBlankNameFails() = runTest(testDispatcher) {
        val result = savePlateUseCase(name = "   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, fakeRepository.savedPlates.size)
    }

    @Test
    fun testSavePlateNormalizesNameBeforeSaving() = runTest(testDispatcher) {
        val result = savePlateUseCase(name = "QUERO BRINCAR")
        assertTrue(result.isSuccess)
        assertEquals("Quero brincar", result.getOrNull()?.name)
    }

    @Test
    fun testSavePlateForwardsNullCategoryIds() = runTest(testDispatcher) {
        val result = savePlateUseCase(name = "Bola", categoryIds = null)
        assertTrue(result.isSuccess)
        assertEquals(null, fakeRepository.lastCategoryIds)
    }

    private class FakePlateRepository : PlateRepository {
        val savedPlates = mutableListOf<PlateEntity>()
        var lastCategoryIds: List<String>? = null

        override fun getAllPlates(): Flow<List<PlateEntity>> = flowOf(savedPlates)
        override fun getPlateById(id: String): Flow<PlateEntity?> = flowOf(savedPlates.find { it.id == id })
        override suspend fun getPlateByIdSync(id: String): PlateEntity? = savedPlates.find { it.id == id }
        override fun searchPlates(query: String): Flow<List<PlateEntity>> = flowOf(emptyList())
        override fun getProfilesForPlate(plateId: String): Flow<List<com.example.elosonoro.data.local.entity.ProfileEntity>> =
            flowOf(emptyList())

        override suspend fun savePlate(
            name: String,
            imageUri: String?,
            audioUri: String?,
            id: String?,
            categoryIds: List<String>?
        ): Result<PlateEntity> {
            lastCategoryIds = categoryIds
            val plate = PlateEntity(
                id = id ?: "plate-${savedPlates.size + 1}",
                name = name,
                imageUri = imageUri,
                audioUri = audioUri
            )
            savedPlates.add(plate)
            return Result.success(plate)
        }

        override suspend fun deletePlate(plate: PlateEntity): Result<Unit> = Result.success(Unit)
        override suspend fun deletePlateById(id: String): Result<Unit> = Result.success(Unit)
    }
}

