package com.cloud.sync.data


sealed class SyncUiState {
    object Idle : SyncUiState()
    object PermissionDenied : SyncUiState()
    object Loading : SyncUiState()
    data class Progress(val uploaded: Int, val total: Int) : SyncUiState()
    data class Success(val count: Int) : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}