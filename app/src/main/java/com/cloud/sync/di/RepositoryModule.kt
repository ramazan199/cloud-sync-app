package com.cloud.sync.di

import android.content.Context
import com.cloud.sync.data.repository.GalleryRepositoryImpl
import com.cloud.sync.data.repository.IGalleryRepository
import com.cloud.sync.data.repository.ISyncRepository
import com.cloud.sync.data.repository.SyncRepositoryImpl
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
