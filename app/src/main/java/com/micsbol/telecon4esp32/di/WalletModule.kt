package com.micsbol.telecon4esp32.di

import com.micsbol.telecon4esp32.data.repository.DataStoreWalletRepository
import com.micsbol.telecon4esp32.data.repository.DurableExplorerGiftRepository
import com.micsbol.telecon4esp32.domain.repository.IExplorerGiftRepository
import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WalletModule {

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: DataStoreWalletRepository,
    ): IWalletRepository

    @Binds
    @Singleton
    abstract fun bindExplorerGiftRepository(
        impl: DurableExplorerGiftRepository,
    ): IExplorerGiftRepository
}
