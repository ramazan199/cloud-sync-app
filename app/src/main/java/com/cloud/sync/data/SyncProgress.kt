package com.cloud.sync.data

// Progress state for the SyncStatusManager
data class SyncProgress(
    val isSyncing: Boolean = false,
    val text: String = "Ready."
)