package com.cloud.sync.di

import android.content.Context
import com.cloud.sync.repository.GalleryRepositoryImpl
import com.cloud.sync.repository.IGalleryRepository
import com.cloud.sync.repository.ISyncRepository
import com.cloud.sync.repository.SyncRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

    @Provides
    @Singleton
    fun provideGalleryRepository(
        @ApplicationContext context: Context
    ): IGalleryRepository {
        return GalleryRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        @ApplicationContext context: Context
    ): ISyncRepository {
        return SyncRepositoryImpl(context)
    }
}
