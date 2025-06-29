package com.cloud.sync.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.data.ui_state.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Called when the user enters or changes the PIN.
     * It updates the 'pin' in the UI state.
     */
    fun onPinChanged(pin: String) {
        _uiState.update { currentState ->
            currentState.copy(pin = pin, errorMessage = null)
        }
    }

    fun authenticate() {
        viewModelScope.launch {
            // Set loading state to true
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // TODO: Replace this with your actual authentication logic
                delay(2000)

                if (uiState.value.pin == "123456") {
                    _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                } else {
                    // Handle incorrect PIN
                    throw Exception("Invalid PIN. Please try again.")
                }
            } catch (e: Exception) {
                // Handle any errors during authentication
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unknown error occurred."
                    )
                }
            }
        }
    }
}
