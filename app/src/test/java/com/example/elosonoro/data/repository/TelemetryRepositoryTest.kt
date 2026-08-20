package com.example.elosonoro.data.repository

import com.example.elosonoro.data.local.dao.TelemetryDao
import com.example.elosonoro.data.local.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TelemetryRepositoryTest {

    private lateinit var fakeTelemetryDao: FakeTelemetryDao
    private lateinit var repository: TelemetryRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        fakeTelemetryDao = FakeTelemetryDao()
        repository = TelemetryRepositoryImpl(
            telemetryDao = fakeTelemetryDao,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testRecordPlateUsageInsertsTelemetrySilently() = runTest(testDispatcher) {
        val result = repository.recordPlateUsage(plateId = "plate-100", timestamp = 123456789L)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeTelemetryDao.telemetryList.size)
        val entry = fakeTelemetryDao.telemetryList.first()
        assertEquals("plate-100", entry.plateId)
        assertEquals(123456789L, entry.timestamp)
    }

    @Test
    fun testRecordPlateUsageBlankIdDoesNotInsert() = runTest(testDispatcher) {
        val result = repository.recordPlateUsage(plateId = "   ")

        assertTrue(result.isSuccess)
        assertTrue(fakeTelemetryDao.telemetryList.isEmpty())
    }

    @Test
    fun testClearAllTelemetryRemovesRecords() = runTest(testDispatcher) {
        fakeTelemetryDao.telemetryList.add(TelemetryEntity(plateId = "plate-1", timestamp = 1L))
        fakeTelemetryDao.telemetryList.add(TelemetryEntity(plateId = "plate-2", timestamp = 2L))

        val result = repository.clearAllTelemetry()

        assertTrue(result.isSuccess)
        assertTrue(fakeTelemetryDao.telemetryList.isEmpty())
    }

    private class FakeTelemetryDao : TelemetryDao {
        val telemetryList = mutableListOf<TelemetryEntity>()

        override suspend fun insert(telemetry: TelemetryEntity): Long {
            telemetryList.add(telemetry)
            return telemetryList.size.toLong()
        }
        override fun getAllTelemetry(): Flow<List<TelemetryEntity>> =
            flowOf(telemetryList)
        override fun getTelemetryForPlate(plateId: String): Flow<List<TelemetryEntity>> =
            flowOf(telemetryList.filter { it.plateId == plateId })
        override fun getCountForPlate(plateId: String): Flow<Int> =
            flowOf(telemetryList.count { it.plateId == plateId })
        override suspend fun clearAll() {
            telemetryList.clear()
        }
    }
}
