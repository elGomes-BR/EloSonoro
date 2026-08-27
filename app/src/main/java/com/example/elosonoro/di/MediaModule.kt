package com.example.elosonoro.di

import com.example.elosonoro.data.media.AudioPlayer
import com.example.elosonoro.data.media.AudioPlayerManager
import com.example.elosonoro.data.media.AudioRecorder
import com.example.elosonoro.data.media.AudioRecorderManager
import com.example.elosonoro.data.media.FileManager
import com.example.elosonoro.data.media.LocalFileManager
import com.example.elosonoro.data.media.SoundPlaybackCoordinator
import com.example.elosonoro.data.media.TtsManager
import com.example.elosonoro.data.media.TtsPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val mediaModule = module {
    single<FileManager> { LocalFileManager(androidContext()) }
    single<AudioPlayer> { AudioPlayerManager(androidContext()) }
    single<AudioRecorder> { AudioRecorderManager(androidContext()) }
    single<TtsPlayer> { TtsManager(androidContext()) }
    single { SoundPlaybackCoordinator(get(), get(), get()) }
}
