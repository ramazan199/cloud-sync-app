package com.cloud.sync.view_model


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Represents the state of the authentication screen
data class AuthUiState(
    val pin: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    // Private mutable state flow
    private val _uiState = MutableStateFlow(AuthUiState())
    // Public read-only state flow
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

    /**
     * Simulates the authentication process.
     * This is where you would put your actual authentication logic.
     */
    fun authenticate() {
        viewModelScope.launch {
            // Set loading state to true
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // --- YOUR AUTHENTICATION LOGIC GOES HERE ---
                // For demonstration, we'll simulate a network call
                delay(2000) // Simulate a 2-second delay

                // Example: Check if the PIN is correct (e.g., "123456")
                if (uiState.value.pin == "123456") {
                    _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                } else {
                    // Handle incorrect PIN
                    throw Exception("Invalid PIN. Please try again.")
                }
                // --- END OF YOUR AUTHENTICATION LOGIC ---

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
