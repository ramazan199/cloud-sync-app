package com.cloud.sync.di

import com.cloud.sync.service.BackgroundSyncManager
import com.cloud.sync.service.IBackgroundSyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//application-wide singletons
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindBackgroundSyncManager(
        impl: BackgroundSyncManager
    ): IBackgroundSyncManager
}