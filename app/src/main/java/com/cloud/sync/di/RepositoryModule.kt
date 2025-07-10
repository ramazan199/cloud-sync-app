package com.cloud.sync.di

import com.cloud.sync.data.local.secure.TokenStorage
import com.cloud.sync.data.local.datastore.SyncPreferencesDataSource
import com.cloud.sync.data.local.mediastore.PhotoLocalDataSource
import com.cloud.sync.data.repository.GalleryRepositoryImpl
import com.cloud.sync.data.repository.OauthTokenRepository
import com.cloud.sync.data.repository.SyncRepositoryImpl
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.IOauthTokenRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGalleryRepository(
        photoLocalDataSource: PhotoLocalDataSource
    ): IGalleryRepository {
        return GalleryRepositoryImpl(photoLocalDataSource)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        syncPreferencesDataSource: SyncPreferencesDataSource
    ): ISyncRepository {
        return SyncRepositoryImpl(syncPreferencesDataSource)
    }

    @Provides
    @Singleton
    fun provideOauthTokenRepository(
        tokenStorage: TokenStorage
    ): IOauthTokenRepository {
        return OauthTokenRepository(tokenStorage)
    }
}
