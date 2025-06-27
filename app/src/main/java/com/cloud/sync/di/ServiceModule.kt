package com.cloud.sync.di

import com.cloud.sync.service.IPermissionsManager
import com.cloud.sync.service.IQRScanner
import com.cloud.sync.service.PermissionsManager
import com.cloud.sync.service.QRScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped


@Module
@InstallIn(ViewModelComponent::class)
abstract class ServiceModule {

    @Binds
    @ViewModelScoped
    abstract fun bindPermissionsManager(
        impl: PermissionsManager
    ): IPermissionsManager

    @Binds
    @ViewModelScoped
    //TODO: change to class level binding
    abstract fun bindQRScanner(
        impl: QRScanner
    ): IQRScanner

}
