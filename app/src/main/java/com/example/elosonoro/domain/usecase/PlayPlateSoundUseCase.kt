package com.example.elosonoro.domain.usecase

import com.example.elosonoro.data.local.entity.PlateEntity
import com.example.elosonoro.data.media.SoundPlaybackCoordinator
import com.example.elosonoro.domain.repository.TelemetryRepository

/**
 * Caso de uso para reprodução sonora condicional com registro silencioso de telemetria.
 */
class PlayPlateSoundUseCase(
    private val playbackCoordinator: SoundPlaybackCoordinator,
    private val telemetryRepository: TelemetryRepository
) {

    suspend operator fun invoke(plate: PlateEntity): Boolean {
        val played = playbackCoordinator.playPlate(plate)
        if (played && plate.id.isNotBlank()) {
            // Coleta de telemetria silenciosa em segundo plano
            telemetryRepository.recordPlateUsage(plate.id)
        }
        return played
    }
}
