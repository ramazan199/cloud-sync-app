package com.cloud.sync.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cloud.sync.background.FullScanService
import com.cloud.sync.background.PhotoSyncWorker
import com.cloud.sync.domain.model.TimeInterval
import com.cloud.sync.domain.repositroy.ISyncRepository
import com.cloud.sync.manager.interfaces.IBackgroundSyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


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

        workManager.enqueueUniquePeriodicWork("UniquePhotoSyncWork", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override suspend fun cancelPeriodicSync() {
        syncRepository.deleteSyncFromNowPoint()
        workManager.cancelAllWorkByTag(periodicWorkTag)
    }

    override fun startFullScanService() {
        val intent = Intent(context, FullScanService::class.java).apply {
            action = FullScanService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopFullScanService() {
        val intent = Intent(context, FullScanService::class.java).apply {
            action = FullScanService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun getPeriodicSyncWorkInfoFlow(): Flow<Boolean> {
        return workManager.getWorkInfosByTagFlow(periodicWorkTag)
            .map { it.any { info -> info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING } }
    }
}