package com.cloud.sync.zother

import android.content.Context
//import com.cloud.photo_optimizer.services.ResizeService
import javax.inject.Inject
import javax.inject.Singleton

// Synchronizer.kt
@Singleton
class Synchronizer @Inject constructor(
    private val context: Context
) {
    private var currentScanResult: String? = null

    fun processScanResult(scanResult: String) {
        currentScanResult = scanResult
        // Add any processing logic here
    }

//    fun startSync() {
//        currentScanResult?.let { result ->
//            // Use the scan result in your sync process
//            val serviceIntent = Intent(context, ResizeService::class.java).apply {
//                putExtra("SCAN_RESULT", result)
//            }
//            ContextCompat.startForegroundService(context, serviceIntent)
//        } ?: throw IllegalStateException("No scan result available")
//    }
}
