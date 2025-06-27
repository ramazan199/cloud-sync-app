package com.cloud.sync.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cloud.sync.data.GalleryPhoto
import com.cloud.sync.data.TimeInterval
import com.cloud.sync.repository.IGalleryRepository
import com.cloud.sync.repository.ISyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PhotoSyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    @Inject
    lateinit var syncRepository: ISyncRepository

    @Inject
    lateinit var galleryRepository: IGalleryRepository
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

            // Define the specific action for what to do when a batch is saved.
            val onBatchSave: suspend (Long) -> Unit = { newTimestamp ->
                val updatedInterval = fromNowInterval.copy(end = newTimestamp)
                allIntervals[fromNowIntervalIndex] = updatedInterval
                syncRepository.saveSyncedIntervals(mergeIntervals(allIntervals))
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

    private fun mergeIntervals(intervals: List<TimeInterval>): List<TimeInterval> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.start }
        val merged = mutableListOf<TimeInterval>()
        var current = sorted.first()
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.start <= current.end + 1) {
                current = current.copy(end = maxOf(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }


    private suspend fun syncAndSaveInBatches(
        photos: List<GalleryPhoto>,
        initialTimestamp: Long,
        onBatchSave: suspend (Long) -> Unit
    ) {
        val batchSize = 10
        var photosInBatch = 0
        var lastSyncedTimestamp = initialTimestamp

        for (photo in photos) {
            // Check for cancellation before processing each photo
            withContext(Dispatchers.IO) {
                // TODO: FileUploader.startSendFileAsync(File(photo.path))
                //  include path to GalleryPhoto class & include communicationLib in build gradle
                delay(1000)
                println("Uploaded ${photo.displayName}")

            }
            lastSyncedTimestamp = photo.dateAdded

            photosInBatch++
            if (photosInBatch >= batchSize) {
                onBatchSave(lastSyncedTimestamp)
                photosInBatch = 0 // Reset for the next batch
            }
        }

        // Save any remaining photos that didn't make a full batch
        if (photosInBatch > 0) {
            onBatchSave(lastSyncedTimestamp)
        }
    }
}