package com.example.elosonoro.di

import com.example.elosonoro.data.remote.api.ArasaacApiService
import com.example.elosonoro.data.repository.ArasaacRepositoryImpl
import com.example.elosonoro.domain.repository.ArasaacRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {

    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("https://api.arasaac.org/")
            .client(get<OkHttpClient>())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<ArasaacApiService> {
        get<Retrofit>().create(ArasaacApiService::class.java)
    }

    single<ArasaacRepository> {
        ArasaacRepositoryImpl(
            apiService = get(),
            fileManager = get()
        )
    }
}
