package com.cloud.sync.manager.interfaces

import android.content.Intent

interface IAuthManager {
    fun getAuthIntent(): Intent
    suspend fun exchangeCodeForToken(intent: Intent)
}