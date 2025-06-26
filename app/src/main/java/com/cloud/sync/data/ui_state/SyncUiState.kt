package com.cloud.sync.data.ui_state

// UI State class for the ViewModel
data class SyncUiState(
    val isFullScanInProgress: Boolean = false,
    val isBackgroundSyncScheduled: Boolean = false,
    val statusText: String = "Ready.",
)