package com.cloud.sync.ui.scan

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data object PermissionDenied : ScanUiState()
    data object PermissionGranted : ScanUiState()
    data class Scanned(val content: String?) : ScanUiState()
}