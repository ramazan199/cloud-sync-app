package com.cloud.sync.view_model

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.data.SyncUiState
import com.cloud.sync.repository.IMediaRepository
import com.cloud.sync.service.IPermissionsManager
import com.cloud.sync.service.PermissionSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val permissionManager: IPermissionsManager,
    private val mediaRepository: IMediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val uiState: StateFlow<SyncUiState> = _uiState

    private val requiredPermissionSet = PermissionSet.Storage

    fun setPermissionLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        permissionManager.setLauncher(launcher)
    }

    fun onSyncButtonClicked(context: Context) {
        if (checkPermissions(context)) {
            startSync(context)
        } else {
            requestPermissions()
        }
    }

    fun handlePermissionResult(permissions: Map<String, Boolean>, context: Context) {
        // permissions is Map<String, Boolean> from callback, check if all required are granted
        if (permissions.hasRequiredPermissions()) {
            startSync(context)
        } else {
            updateUiState(SyncUiState.PermissionDenied)
        }
    }

    private fun checkPermissions(context: Context): Boolean {
        return permissionManager.hasPermissions(context, requiredPermissionSet)
    }

    private fun requestPermissions() {
        permissionManager.requestPermissions(requiredPermissionSet)
    }

    private fun Map<String, Boolean>.hasRequiredPermissions(): Boolean {
        return requiredPermissionSet.permissions.all { this.getOrDefault(it, false) }
    }

    private fun startSync(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val photoUris = mediaRepository.getPhotoUris(context)
                val uploadedCount = uploadPhotos(photoUris)
                updateUiState(SyncUiState.Success(uploadedCount))
            } catch (e: Exception) {
                updateUiState(SyncUiState.Error(e.localizedMessage ?: "Unknown error"))
            }
        }
    }


    private suspend fun uploadPhotos(uris: List<Uri>): Int {
        var uploadedCount = 0
        val total = uris.size

        for (uri in uris) {
            delay(500) // Simulate upload
            uploadedCount++

            updateUiState(SyncUiState.Progress(uploadedCount, total))
        }

        return uploadedCount
    }

    private fun updateUiState(state: SyncUiState) {
        viewModelScope.launch {
            _uiState.emit(state)
        }
    }
}





