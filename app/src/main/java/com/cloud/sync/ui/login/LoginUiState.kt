package com.cloud.sync.ui.login


sealed interface LoginUiState {
    data object Loading : LoginUiState
    data object Authenticated : LoginUiState
    data object Unauthenticated : LoginUiState
}