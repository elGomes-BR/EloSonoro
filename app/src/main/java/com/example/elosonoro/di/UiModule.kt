package com.example.elosonoro.di

import com.example.elosonoro.data.preferences.PreferencesManager
import com.example.elosonoro.data.preferences.UserPreferences
import com.example.elosonoro.ui.arasaac.ArasaacSearchViewModel
import com.example.elosonoro.ui.board.BoardViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {

    single<UserPreferences> { PreferencesManager(androidContext()) }
    single { get<UserPreferences>() as PreferencesManager }

    viewModel {
        BoardViewModel(
            getPlatesUseCase = get(),
            getProfilesWithPlatesUseCase = get(),
            playPlateSoundUseCase = get(),
            savePlateUseCase = get(),
            createProfileUseCase = get(),
            updateProfileUseCase = get(),
            plateRepository = get(),
            profileRepository = get(),
            preferencesManager = get()
        )
    }

    viewModel {
        ArasaacSearchViewModel(
            arasaacRepository = get()
        )
    }
}
