package com.cloud.sync.ui.login


import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.manager.interfaces.IAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: IAuthManager
) : ViewModel() {

    fun getAuthIntent(): Intent {
        return authManager.getAuthIntent()
    }

    fun handleAuthResult(intent: Intent) {
        viewModelScope.launch {
            authManager.exchangeCodeForToken(intent)
        }
    }
}