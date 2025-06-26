package com.cloud.sync.view_model

import PhotoSyncWorker
import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.cloud.sync.data.ui_state.SyncUiState
import com.cloud.sync.data.TimeInterval
import com.cloud.sync.repository.ISyncRepository
import com.cloud.sync.service.FullScanService
import com.cloud.sync.service.SyncStatusManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    @Inject
    lateinit var syncRepository: ISyncRepository

    private val workManager = WorkManager.getInstance(context)
    private val workRequestTag = "periodic-photo-sync"

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        // Observe background worker status
        workManager.getWorkInfosByTagFlow(workRequestTag)
            .map { it.any { info -> info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING } }
            .map { isScheduled -> _uiState.update { it.copy(isBackgroundSyncScheduled = isScheduled) } }
            .launchIn(viewModelScope)

        // Observe full scan progress from the service via the manager
        viewModelScope.launch {
            SyncStatusManager.progress.collect { progress ->
                _uiState.update { it.copy(isFullScanInProgress = progress.isSyncing, statusText = progress.text) }
            }
        }
    }

    fun onFromNowSyncToggled(isEnabled: Boolean) {
        viewModelScope.launch {
            if (isEnabled) scheduleFromNowSync() else cancelFromNowSync()
        }
    }

    fun startFullScan() {
        val intent = Intent(context, FullScanService::class.java).apply { action = FullScanService.ACTION_START }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopFullScan() {
        val intent = Intent(context, FullScanService::class.java).apply { action = FullScanService.ACTION_STOP }
        context.startService(intent)
    }


    private suspend fun scheduleFromNowSync() {
        // Check if this is the very first time the user is enabling this feature.
        if (syncRepository.syncFromNowPoint.first() == 0L) {
            // Get the current time in seconds, which is what MediaStore uses.
            val syncStartTime = System.currentTimeMillis() / 1000

            // Create the new anchor interval using the current time.
            val newInterval = TimeInterval(start = syncStartTime, end = syncStartTime)
            val currentIntervals = syncRepository.syncedIntervals.first()
            val allIntervals = currentIntervals + newInterval

            syncRepository.saveSyncFromNowPoint(syncStartTime)
            syncRepository.saveSyncedIntervals(allIntervals)
        }

        val request = PeriodicWorkRequestBuilder<PhotoSyncWorker>(15, TimeUnit.MINUTES) // Use a reasonable interval
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .addTag(workRequestTag)
            .build()
        workManager.enqueueUniquePeriodicWork("UniquePhotoSyncWork", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private suspend fun cancelFromNowSync() {
        syncRepository.deleteSyncFromNowPoint()
        workManager.cancelAllWorkByTag(workRequestTag)
    }
}