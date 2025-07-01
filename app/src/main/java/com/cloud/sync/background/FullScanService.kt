package com.cloud.sync.background

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cloud.sync.common.SyncStatusManager
import com.cloud.sync.manager.interfaces.IFullScanProcessManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.coroutines.coroutineContext // Required for coroutineContext extension property

@AndroidEntryPoint
class FullScanService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var fullScanProcessor: IFullScanProcessManager

    private lateinit var notificationManager: NotificationManager

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_CHANNEL_ID = "FullScanChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Create notification channel for ongoing service updates.
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Full Sync Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (!SyncStatusManager.isSyncing()) {
                startServiceLogic()
            }
            ACTION_STOP -> stopSync()
        }
        return START_NOT_STICKY
    }

    /**
     * Initiates the full scan logic and starts observing processor status updates.
     */
    private fun startServiceLogic() {
        // Collect status updates from the processor to manage the foreground notification.
        serviceScope.launch {
            SyncStatusManager.progress.collect { progress ->
                updateForegroundNotification(progress.text)
            }
        }
        // Launch the core full scan operation in a separate coroutine.
        serviceScope.launch {
            startFullScanLogicInternal()
        }
    }

    /**
     * Stops the full scan, cancels ongoing coroutines, and removes the foreground notification.
     */
    private fun stopSync() {
        serviceScope.coroutineContext.cancelChildren()
        SyncStatusManager.update(false, "Full scan stopped.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Executes the main full scan and synchronization logic.
     * This function delegates core operations to [fullScanProcessor].
     */
    private suspend fun startFullScanLogicInternal() {
        SyncStatusManager.update(true, "Preparing full scan...")

        try {
            var allIntervals = fullScanProcessor.initializeIntervals()

            // Process gaps and merge intervals until less than two remain.
            while (allIntervals.size >= 2) {
                coroutineContext.ensureActive() // Ensure the coroutine is still active for cancellation.
                allIntervals = fullScanProcessor.processNextTwoIntervals(allIntervals, coroutineContext)
            }

            coroutineContext.ensureActive() // Ensure active before processing tail.
            // Process any remaining photos at the end of the timeline.
            fullScanProcessor.processTailEnd(allIntervals, coroutineContext)

            SyncStatusManager.update(false, "Full scan complete!")
        } catch (e: Exception) {
            // Re-throw CancellationException to propagate cancellation correctly.
            if (e is CancellationException) throw e
            SyncStatusManager.update(false, "Error: ${e.message}")
        } finally {
            // Ensure service stops and notification is removed even if an error occurs.
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * Updates the persistent foreground notification with the current sync status.
     * @param text The status message to display in the notification.
     */
    private fun updateForegroundNotification(text: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Gallery Sync")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true) // Makes the notification non-dismissible.
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification) // Keep service in foreground.
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(NOTIFICATION_ID) // for reliable notification cancellation
        serviceScope.cancel() // Cancel all coroutines when the service is destroyed.
    }
}