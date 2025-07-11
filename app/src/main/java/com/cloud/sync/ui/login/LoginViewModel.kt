package com.cloud.sync.ui.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.domain.repositroy.IOauthTokenRepository
import com.cloud.sync.manager.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val oauthTokenRepository: IOauthTokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            if (!oauthTokenRepository.getAccessToken().isNullOrBlank()) {
                _uiState.value = LoginUiState.Authenticated
            } else {
                _uiState.value = LoginUiState.Unauthenticated
            }
        }
    }

    fun getAuthIntent(): Intent {
        return authManager.getAuthIntent()
    }

    suspend fun handleAuthResult(intent: Intent) {
        authManager.exchangeCodeForToken(intent)
    }
}