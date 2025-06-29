package com.cloud.sync.domain.model

// Progress state for the SyncStatusManager
data class SyncProgress(
    val isSyncing: Boolean = false,
    val text: String = "Ready."
)