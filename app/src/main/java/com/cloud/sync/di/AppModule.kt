package com.cloud.sync.di

import com.cloud.sync.mananager.BackgroundSyncManager
import com.cloud.sync.mananager.IBackgroundSyncManager
import com.cloud.sync.mananager.ISyncIntervalManager
import com.cloud.sync.mananager.SyncIntervalManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

//application-wide singletons
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindBackgroundSyncManager(
        impl: BackgroundSyncManager
    ): IBackgroundSyncManager

    @Binds
    abstract fun bindSyncIntervalManager(
        impl: SyncIntervalManager
    ): ISyncIntervalManager
}