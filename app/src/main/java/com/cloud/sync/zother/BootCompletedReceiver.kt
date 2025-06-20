package com.cloud.sync.zother

// BootCompletedReceiver.kt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ServiceBgUpload::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}