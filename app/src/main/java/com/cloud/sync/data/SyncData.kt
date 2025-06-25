package com.cloud.sync.data


import kotlinx.serialization.Serializable

// Data class to hold a synced time interval.
@Serializable
data class TimeInterval(val start: Long, val end: Long)

// Data class to represent a photo from the gallery
data class GalleryPhoto(
    val id: Long,
    val dateAdded: Long, // Timestamp in seconds
    val displayName: String
)

// UI State class for the ViewModel
data class SyncUiState(
    val isFullScanInProgress: Boolean = false,
    val isBackgroundSyncScheduled: Boolean = false,
    val statusText: String = "Ready.",
)

// Progress state for the SyncStatusManager
data class SyncProgress(
    val isSyncing: Boolean = false,
    val text: String = "Ready."
)