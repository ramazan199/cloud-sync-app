package com.cloud.sync

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.cloud.sync.ui.theme.AppTheme
import com.cloud.sync.zother.ServiceBgUpload
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme (dynamicColor = false){
                AppNavigation()
            }
        }

        val serviceIntent = Intent(this, ServiceBgUpload::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}