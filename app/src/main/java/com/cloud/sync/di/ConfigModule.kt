package com.cloud.sync.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.cloud.sync.config.SyncConfig
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideSyncConfig(): SyncConfig {
        return SyncConfig()
    }
}
