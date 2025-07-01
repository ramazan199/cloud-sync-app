package com.cloud.sync.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.cloud.sync.manager.interfaces.IPermissionsManager
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Concrete implementation of [IPermissionsManager].
 * Manages permission requests and checks for granted permissions.
 */
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
        val CAMERA = setOf(Manifest.permission.CAMERA)
        val STORAGE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            setOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // specific permission sets
    object Camera : PermissionSet(CAMERA)
    object Storage : PermissionSet(STORAGE)

    // Custom permission set constructor
    class Custom(permissions: Set<String>) : PermissionSet(permissions)
}