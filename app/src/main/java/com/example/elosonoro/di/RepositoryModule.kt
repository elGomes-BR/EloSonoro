package com.example.elosonoro.di

import com.example.elosonoro.data.repository.PlateRepositoryImpl
import com.example.elosonoro.data.repository.ProfileRepositoryImpl
import com.example.elosonoro.data.repository.TelemetryRepositoryImpl
import com.example.elosonoro.domain.repository.PlateRepository
import com.example.elosonoro.domain.repository.ProfileRepository
import com.example.elosonoro.domain.repository.TelemetryRepository
import org.koin.dsl.module

val repositoryModule = module {

    single<PlateRepository> {
        PlateRepositoryImpl(
            plateDao = get(),
            fileManager = get(),
            crossRefDao = get(),
            database = get()
        )
    }

    single<ProfileRepository> {
        ProfileRepositoryImpl(
            profileDao = get(),
            crossRefDao = get()
        )
    }

    single<TelemetryRepository> {
        TelemetryRepositoryImpl(
            telemetryDao = get()
        )
    }
}
