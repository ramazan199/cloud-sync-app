package com.cloud.sync.service

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

interface IPermissionsManager {
    fun setLauncher(launcher: ActivityResultLauncher<Array<String>>)
    fun requestPermissions(permissionSet: PermissionSet)
    fun hasPermissions(context: Context, permissionSet: PermissionSet): Boolean
}

@ViewModelScoped
class PermissionsManager @Inject constructor() : IPermissionsManager {
    private var launcher: ActivityResultLauncher<Array<String>>? = null

    override fun setLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        this.launcher = launcher
    }

    override fun requestPermissions(permissionSet: PermissionSet) {
        launcher?.launch(permissionSet.permissions.toTypedArray())
    }

    override fun hasPermissions(context: Context, permissionSet: PermissionSet): Boolean {
        return hasPermissions(context, permissionSet.permissions)
    }

    private fun hasPermissions(context: Context, permissions: Set<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

sealed class PermissionSet(val permissions: Set<String>) {
    companion object {
        // Common permission sets
        val CAMERA = setOf(android.Manifest.permission.CAMERA)
        val STORAGE = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            setOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            setOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Define your specific permission sets here
    object Camera : PermissionSet(CAMERA)
    object Storage : PermissionSet(STORAGE)

    // Custom permission set constructor
    class Custom(permissions: Set<String>) : PermissionSet(permissions)
}