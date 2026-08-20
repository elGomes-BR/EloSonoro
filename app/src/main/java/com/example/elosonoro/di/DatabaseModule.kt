package com.example.elosonoro.di

import androidx.room.Room
import com.example.elosonoro.data.local.EloSonoroDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            EloSonoroDatabase::class.java,
            EloSonoroDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}
