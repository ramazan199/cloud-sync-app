package com.cloud.sync.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cloud.sync.data.GalleryPhoto
import com.cloud.sync.data.TimeInterval
import com.cloud.sync.repository.IGalleryRepository
import com.cloud.sync.repository.ISyncRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class FullScanService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Inject
    lateinit var syncIntervalRepository: ISyncRepository
    @Inject
    lateinit var galleryRepository: IGalleryRepository
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val ACTION_START = "ACTION_START";
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_CHANNEL_ID = "FullScanChannel";
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (!SyncStatusManager.isSyncing()) {
                serviceScope.launch { startFullScanLogic() }
            }

            ACTION_STOP -> stopSync()
        }
        return START_STICKY
    }

    private fun stopSync() {
        serviceScope.coroutineContext.cancelChildren()
        SyncStatusManager.update(false, "Full scan stopped.")
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    private suspend fun startFullScanLogic() {
        startForeground(NOTIFICATION_ID, createNotification("Starting scan..."))
        SyncStatusManager.update(true, "Preparing full scan...")

        try {
//            repository.clearAllData() //TODO: remove clear its for testing purpose.
            var allIntervals = syncIntervalRepository.syncedIntervals.first().toMutableList()
            if (allIntervals.none { it.start == 0L }) allIntervals.add(0, TimeInterval(0, 0))
            allIntervals.sortBy { it.start }

            while (allIntervals.size >= 2) {
                coroutineContext.ensureActive()

                val interval1 = allIntervals[0]
                val interval2 = allIntervals[1]

                val photosInGap =
                    galleryRepository.getPhotosInInterval(interval1.end + 1, interval2.start - 1)

                if (photosInGap.isNotEmpty()) {
                    // Define a simple progress-saving lambda. Its ONLY job is to update interval1's end.
                    val onBatchSave: suspend (Long) -> Unit = { newEndTimestamp ->
                        val updatedInterval1 = interval1.copy(end = newEndTimestamp)
                        allIntervals[0] = updatedInterval1 // Update the interval in the list
                        syncIntervalRepository.saveSyncedIntervals(allIntervals) // Save the entire list for crash recovery
                    }

                    // Pass the progress-saving lambda to the sync function.
                    syncAndSaveInBatches(
                        coroutineContext,
                        photosInGap,
                        "Syncing gap...",
                        onBatchSave
                    )
                }

                // --- MERGE LOGIC ---
                // This code runs AFTER the entire gap is synced (or if there was no gap).
                val finalInterval1 = allIntervals[0] // This will be the original or the updated one
                val finalInterval2 = allIntervals[1]
                val merged = TimeInterval(
                    finalInterval1.start,
                    maxOf(finalInterval1.end, finalInterval2.end)
                )//consider we have some malfunction and interval1 is (start: 100, end: 800) interval2 is (start: 500, end: 600) so we need to take max of them.

                allIntervals.removeAt(0)
                allIntervals.removeAt(0)
                allIntervals.add(0, merged)
                // Save the final merged state
                syncIntervalRepository.saveSyncedIntervals(allIntervals)
            }

            // The rest of the logic for syncing the tail end
            coroutineContext.ensureActive()
            val finalInterval = allIntervals.first()
            val photosInTail = galleryRepository.getPhotos(startTimeSeconds = finalInterval.end + 1)
            if (photosInTail.isNotEmpty()) {
                val onBatchSave: suspend (Long) -> Unit = { newEndTimestamp ->
                    val updatedInterval = finalInterval.copy(end = newEndTimestamp)
                    allIntervals[0] = updatedInterval
                    syncIntervalRepository.saveSyncedIntervals(allIntervals)
                }
                syncAndSaveInBatches(coroutineContext, photosInTail, "Finalizing...", onBatchSave)
            }

            SyncStatusManager.update(false, "Full scan complete!")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            SyncStatusManager.update(false, "Error: ${e.message}")
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        }
    }


    private suspend fun syncAndSaveInBatches(
        context: CoroutineContext,
        photos: List<GalleryPhoto>,
        statusPrefix: String,
        onBatchSave: suspend (Long) -> Unit
    ) {
        val batchSize = 10
        var photosInBatch = 0
        var lastSyncedTimestamp = 0L

        photos.forEachIndexed { index, photo ->
            context.ensureActive()
            // TODO: FileUploader.startSendFileAsync(File(photo.path))
            //  include path to GalleryPhoto class & include communicationLib in build gradle            delay(1000)
            println("Uploaded ${photo.displayName}")

            lastSyncedTimestamp = photo.dateAdded
            updateStatus(true, "$statusPrefix (${index + 1}/${photos.size})")

            if (++photosInBatch >= batchSize) {
                onBatchSave(lastSyncedTimestamp)
                photosInBatch = 0
            }
        }
        // Save any remaining photos that didn't make a full batch
        if (photosInBatch > 0) {
            onBatchSave(lastSyncedTimestamp)
        }
    }

    private fun updateStatus(isSyncing: Boolean, text: String) {
        SyncStatusManager.update(isSyncing, text)
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun createNotification(text: String): Notification {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Full Sync Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Gallery Sync").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Replace with icon
            .setOngoing(true).build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}