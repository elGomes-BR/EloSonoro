package com.example.elosonoro.domain.repository

import com.example.elosonoro.data.local.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositório para coleta silenciosa e consulta de telemetria e métricas de uso.
 */
interface TelemetryRepository {

    /**
     * Registra silenciosamente o acionamento de uma placa em segundo plano.
     */
    suspend fun recordPlateUsage(plateId: String, timestamp: Long = System.currentTimeMillis()): Result<Unit>

    fun getAllTelemetry(): Flow<List<TelemetryEntity>>

    fun getTelemetryForPlate(plateId: String): Flow<List<TelemetryEntity>>

    fun getPlateUsageCount(plateId: String): Flow<Int>

    suspend fun clearAllTelemetry(): Result<Unit>
}
