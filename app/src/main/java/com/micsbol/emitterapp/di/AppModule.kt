package com.micsbol.emitterapp.di

import com.micsbol.emitterapp.data.bluetooth.AndroidBluetoothController
import com.micsbol.emitterapp.data.repository.AndroidCodeAssetRepository
import com.micsbol.emitterapp.data.repository.SettingsRepository
import com.micsbol.emitterapp.domain.bluetooth.RemoteController
import com.micsbol.emitterapp.domain.repository.ICodeAssetRepository
import com.micsbol.emitterapp.domain.repository.ISettingsRepository
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

    @Binds
    @Singleton
    abstract fun bindCodeAssetRepository(
        impl: AndroidCodeAssetRepository
    ): ICodeAssetRepository
}
