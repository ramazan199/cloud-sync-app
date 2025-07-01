package com.cloud.sync.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cloud.sync.common.config.SyncConfig
import com.cloud.sync.domain.model.GalleryPhoto
import com.cloud.sync.domain.repositroy.IGalleryRepository
import com.cloud.sync.domain.repositroy.ISyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class PhotoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: ISyncRepository,
    private val galleryRepository: IGalleryRepository,
    private val syncConfig: SyncConfig
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        println("Worker: Starting periodic 'From Now' sync check.")
        try {
            val syncPoint = syncRepository.syncFromNowPoint.first()
            if (syncPoint == 0L) {
                println("Worker: 'Sync From Now' is not set up. Skipping.")
                return Result.success()
            }

            val allIntervals = syncRepository.syncedIntervals.first().toMutableList()
            val fromNowIntervalIndex = allIntervals.indexOfFirst { it.start == syncPoint }
            if (fromNowIntervalIndex == -1) {
                println("Worker: Error! Anchor interval not found. Failing job.")
                return Result.failure()
            }

            val fromNowInterval = allIntervals[fromNowIntervalIndex]
            val photosToSync =
                galleryRepository.getPhotos(startTimeSeconds = fromNowInterval.end + 1)
            if (photosToSync.isEmpty()) {
                println("Worker: No new photos found.")
                return Result.success()
            }

            println("Worker: Found ${photosToSync.size} new photos. Starting upload...")

            val onBatchSave: suspend (Long) -> Unit = { newTimestamp ->
                val updatedInterval = fromNowInterval.copy(end = newTimestamp)
                allIntervals[fromNowIntervalIndex] = updatedInterval
                syncRepository.saveSyncedIntervals(allIntervals)
                println("Worker: Saved batch progress. New end is $newTimestamp")
            }

            syncAndSaveInBatches(
                photos = photosToSync,
                initialTimestamp = fromNowInterval.end,
                onBatchSave = onBatchSave
            )

            return Result.success()
        } catch (e: Exception) {
            println("Worker: Sync failed with exception: ${e.message}")
            return Result.retry()
        }
    }

    private suspend fun syncAndSaveInBatches(
        photos: List<GalleryPhoto>,
        initialTimestamp: Long,
        onBatchSave: suspend (Long) -> Unit
    ) = coroutineScope {
        val batchSize = syncConfig.batchSize
        var photosInBatch = 0
        var lastSyncedTimestamp = initialTimestamp

        for (photo in photos) {
            ensureActive()

            withContext(Dispatchers.IO) {
                delay(1000)
                println("Uploaded ${photo.displayName}")
            }

            lastSyncedTimestamp = photo.dateAdded
            photosInBatch++

            if (photosInBatch >= batchSize) {
                onBatchSave(lastSyncedTimestamp)
                photosInBatch = 0
            }
        }

        if (photosInBatch > 0) {
            onBatchSave(lastSyncedTimestamp)
        }
    }
}