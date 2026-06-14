package com.micsbol.telecon4esp32.di

import com.micsbol.telecon4esp32.data.bluetooth.AndroidBluetoothController
import com.micsbol.telecon4esp32.data.repository.AndroidCodeAssetRepository
import com.micsbol.telecon4esp32.data.repository.SettingsRepository
import com.micsbol.telecon4esp32.domain.bluetooth.RemoteController
import com.micsbol.telecon4esp32.domain.repository.ICodeAssetRepository
import com.micsbol.telecon4esp32.domain.repository.ISettingsRepository
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
