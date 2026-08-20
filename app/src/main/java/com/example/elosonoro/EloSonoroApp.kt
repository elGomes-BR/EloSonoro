package com.example.elosonoro

import android.app.Application
import com.example.elosonoro.di.databaseModule
import com.example.elosonoro.di.mediaModule
import com.example.elosonoro.di.networkModule
import com.example.elosonoro.di.repositoryModule
import com.example.elosonoro.di.useCaseModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class EloSonoroApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@EloSonoroApp)
            modules(
                listOf(
                    databaseModule,
                    mediaModule,
                    networkModule,
                    repositoryModule,
                    useCaseModule
                )
            )
        }
    }
}
