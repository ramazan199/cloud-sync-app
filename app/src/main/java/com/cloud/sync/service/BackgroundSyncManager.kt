package com.cloud.sync.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cloud.sync.data.TimeInterval
import com.cloud.sync.repository.ISyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Manages the scheduling and cancellation of background photo synchronization tasks,
 * and controls the foreground service for full scans.
 */
interface IBackgroundSyncManager {

    /**
     * Schedules a periodic photo synchronization task to run at a specified interval.
     *
     * If this is the very first time the feature is enabled, it sets the current time
     * as the "sync from now" anchor point, ensuring subsequent syncs only process newer photos.
     * The task requires an active network connection.
     * Uses WorkManager's [ExistingPeriodicWorkPolicy.KEEP] to avoid re-enqueuing if already active.
     */
    suspend fun schedulePeriodicSync()

    /**
     * Cancels the currently scheduled periodic photo synchronization task.
     *
     * Also deletes the saved "sync from now" anchor point, effectively resetting
     * the "from now" sync feature.
     */
    suspend fun cancelPeriodicSync()

    /**
     * Initiates a full photo scan by starting the [FullScanForegroundService] as a foreground service.
     * This operation processes all existing photos on the device.
     */
    fun startFullScanService()

    /**
     * Stops the currently running [FullScanForegroundService], halting any ongoing full scan.
     */
    fun stopFullScanService()

    /**
     * Provides a [Flow] that emits `true` if the periodic photo synchronization worker
     * is currently enqueued or running, and `false` otherwise.
     *
     * Consumers can collect this flow to observe the background sync status.
     *
     * @return A [Flow] of [Boolean] indicating whether the periodic sync is active.
     */
    fun getPeriodicSyncWorkInfoFlow(): Flow<Boolean>
}

@Singleton
class BackgroundSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncRepository: ISyncRepository
) : IBackgroundSyncManager {

    private val workManager: WorkManager = WorkManager.getInstance(context)
    private val periodicWorkTag = "periodic-photo-sync"

    override suspend fun schedulePeriodicSync() {
        if (syncRepository.syncFromNowPoint.first() == 0L) {
            val syncStartTime = System.currentTimeMillis() / 1000L
            val newInterval = TimeInterval(start = syncStartTime, end = syncStartTime)
            val currentIntervals = syncRepository.syncedIntervals.first()
            val allIntervals = currentIntervals + newInterval

            syncRepository.saveSyncFromNowPoint(syncStartTime)
            syncRepository.saveSyncedIntervals(allIntervals)
        }

        val request = PeriodicWorkRequestBuilder<PhotoSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(periodicWorkTag)
            .build()

        workManager.enqueueUniquePeriodicWork("PhotoSyncWork", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override suspend fun cancelPeriodicSync() {
        syncRepository.deleteSyncFromNowPoint()
        workManager.cancelAllWorkByTag(periodicWorkTag)
    }

    override fun startFullScanService() {
        val intent = Intent(context, FullScanForegroundService::class.java).apply { action = FullScanForegroundService.ACTION_START }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopFullScanService() {
        val intent = Intent(context, FullScanForegroundService::class.java).apply { action = FullScanForegroundService.ACTION_STOP }
        context.startService(intent)
    }

    override fun getPeriodicSyncWorkInfoFlow(): Flow<Boolean> {
        return workManager.getWorkInfosByTagFlow(periodicWorkTag)
            .map { it.any { info -> info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING } }
    }
}