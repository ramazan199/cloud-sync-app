//package com.cloud.sync
//import android.content.Intent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.activity.ComponentActivity
//import androidx.activity.result.ActivityResultLauncher
//import androidx.core.content.ContextCompat
//
//class BackgroundService : ComponentActivity () {
////    private val permissionManager = PermissionsManager(this)
//
//    fun start(permissions : Array<String>){
//        val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            val deniedPermissions = permissions.filterValues { !it }.keys
//            if (deniedPermissions.isEmpty()) {
//                // Permissions granted, start foreground service
//                val serviceIntent = Intent(this, com.cloud.photo_optimizer.services.ResizeService::class.java)
//                ContextCompat.startForegroundService(this, serviceIntent)
//            } else {
//                // Show message indicating which permissions were denied
////                permissionManager.showPermissionDeniedMessage(deniedPermissions)
//            }
//        }
//    }
//
//
//
//}