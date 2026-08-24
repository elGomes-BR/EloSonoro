package com.example.elosonoro.di

import com.example.elosonoro.domain.usecase.CreateProfileUseCase
import com.example.elosonoro.domain.usecase.GetPlatesUseCase
import com.example.elosonoro.domain.usecase.GetProfilesWithPlatesUseCase
import com.example.elosonoro.domain.usecase.PlayPlateSoundUseCase
import com.example.elosonoro.domain.usecase.SavePlateUseCase
import com.example.elosonoro.domain.usecase.UpdateProfileUseCase
import org.koin.dsl.module

val useCaseModule = module {

    factory { SavePlateUseCase(plateRepository = get()) }

    factory { CreateProfileUseCase(profileRepository = get()) }

    factory { UpdateProfileUseCase(profileRepository = get()) }

    factory { PlayPlateSoundUseCase(playbackCoordinator = get(), telemetryRepository = get()) }

    factory { GetPlatesUseCase(plateRepository = get()) }

    factory { GetProfilesWithPlatesUseCase(profileRepository = get()) }
}
