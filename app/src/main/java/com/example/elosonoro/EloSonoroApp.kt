package com.example.elosonoro

import android.app.Application
import com.example.elosonoro.di.databaseModule
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
                    databaseModule
                )
            )
        }
    }
}
