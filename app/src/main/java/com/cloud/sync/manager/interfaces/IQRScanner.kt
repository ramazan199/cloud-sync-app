package com.cloud.sync.manager.interfaces

import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.ScanOptions

interface IQRScanner {
    fun setLauncher(launcher: ActivityResultLauncher<ScanOptions>)
    fun startScan()
}