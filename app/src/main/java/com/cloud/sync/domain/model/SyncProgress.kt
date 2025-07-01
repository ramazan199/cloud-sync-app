package com.cloud.sync.domain.model

/**
 * Represents the progress of a synchronization process.
 */
data class SyncProgress(
    val isSyncing: Boolean = false,
    val text: String = "Ready."
)