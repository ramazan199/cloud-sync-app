package com.cloud.sync.di

import com.cloud.sync.data.local.datastore.SyncPreferencesDataSource
import com.cloud.sync.data.local.mediastore.PhotoLocalDataSource
import com.cloud.sync.data.local.secure.SecureCseMasterKeyStorage
import com.cloud.sync.data.repository.GalleryRepositoryImpl
import com.cloud.sync.data.repository.CseMasterKeyRepository
import com.cloud.sync.data.repository.SyncRepositoryImpl
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.ICseMasterKeyRepository
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
    fun provideKeyRepository(
        secureCseMasterKeyStorage: SecureCseMasterKeyStorage
    ): ICseMasterKeyRepository {
        return CseMasterKeyRepository(secureCseMasterKeyStorage)
    }

//    @Binds
//    @Singleton
//    abstract fun bindKeyRepository(impl: KeyRepository): IKeyRepository
}
