package com.micsbol.telecon4esp32.di

import com.micsbol.telecon4esp32.data.billing.PlayBillingEntitlementRepository
import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindEntitlementRepository(
        impl: PlayBillingEntitlementRepository,
    ): IEntitlementRepository
}
