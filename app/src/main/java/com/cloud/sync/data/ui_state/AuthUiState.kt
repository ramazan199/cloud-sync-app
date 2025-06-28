package com.cloud.sync.data.ui_state

// Represents the state of the authentication screen
data class AuthUiState(
    val pin: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)