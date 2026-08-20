package com.example.elosonoro.data.repository

import android.util.Log
import com.example.elosonoro.data.local.dao.TelemetryDao
import com.example.elosonoro.data.local.entity.TelemetryEntity
import com.example.elosonoro.domain.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementação do repositório de Telemetria.
 * Conforme constitution.md (Seção 5):
 * - Coleta silenciosa e em segundo plano de métricas de acionamento de placas e frequência.
 */
class TelemetryRepositoryImpl(
    private val telemetryDao: TelemetryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TelemetryRepository {

    companion object {
        private const val TAG = "TelemetryRepository"
    }

    override suspend fun recordPlateUsage(
        plateId: String,
        timestamp: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        if (plateId.isBlank()) return@withContext Result.success(Unit)

        try {
            val telemetry = TelemetryEntity(
                plateId = plateId,
                timestamp = timestamp
            )
            telemetryDao.insert(telemetry)
            Log.d(TAG, "Telemetria registrada silenciosamente para a placa $plateId em $timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            // Falha na telemetria não deve quebrar a experiência do usuário (log silencioso)
            Log.w(TAG, "Falha ao registrar telemetria: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getAllTelemetry(): Flow<List<TelemetryEntity>> = telemetryDao.getAllTelemetry()

    override fun getTelemetryForPlate(plateId: String): Flow<List<TelemetryEntity>> =
        telemetryDao.getTelemetryForPlate(plateId)

    override fun getPlateUsageCount(plateId: String): Flow<Int> =
        telemetryDao.getCountForPlate(plateId)

    override suspend fun clearAllTelemetry(): Result<Unit> = withContext(ioDispatcher) {
        try {
            telemetryDao.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar telemetria: ${e.message}", e)
            Result.failure(e)
        }
    }
}
