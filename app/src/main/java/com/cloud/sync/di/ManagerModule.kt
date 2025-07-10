package com.cloud.sync.di

import com.cloud.sync.manager.interfaces.IAuthManager
import com.cloud.sync.manager.interfaces.IPermissionsManager
import com.cloud.sync.manager.interfaces.IQRScanner
import com.cloud.sync.manager.AuthManager
import com.cloud.sync.manager.PermissionsManager
import com.cloud.sync.manager.QRScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped


@Module
@InstallIn(ViewModelComponent::class)
abstract class ManagerModule {

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

    @Binds
    @ViewModelScoped
    abstract fun bindAuthManager(
        impl: AuthManager
    ): IAuthManager


}
