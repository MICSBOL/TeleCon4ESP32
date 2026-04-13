package com.example.emitterapp.di

import com.example.emitterapp.data.bluetooth.AndroidBluetoothController
import com.example.emitterapp.data.repository.SettingsRepository
import com.example.emitterapp.domain.bluetooth.RemoteController
import com.example.emitterapp.domain.repository.ISettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindRemoteController(
        impl: AndroidBluetoothController
    ): RemoteController

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepository
    ): ISettingsRepository
}
