package com.cloud.sync.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloud.sync.data.SyncUiState
import com.cloud.sync.view_model.SyncViewModel
import java.util.Base64

//TODO: in SDK 31 >= silent permission denial if permissions are not granted twice in a row. (or don't ask again)
// Add some message saying go to settings and grant permission for storage
// SyncScreen.kt
@Composable
fun SyncScreen(
    content: String?,
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = hiltViewModel()
) {
    var content = "Avcom9x9U5RQ4am2Yzo7kHOJV9zF5slbJdt9Rh0PyuNfcHJveHk="
    var dcontent  =  Base64.getDecoder().decode(content)
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.handlePermissionResult(permissions, context)
    }
    //TODO: sometimes buggy: mentioned above in comments
    LaunchedEffect(Unit) {
        viewModel.setPermissionLauncher(permissionLauncher)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            SyncUiState.Idle -> IdleState { viewModel.onSyncButtonClicked(context, content.toByteArray()) }
            is SyncUiState.Progress -> LoadingState(state.uploaded, state.total)
            is SyncUiState.Success -> SuccessState(state.count)
            SyncUiState.PermissionDenied -> PermissionDeniedState {
                viewModel.onSyncButtonClicked(
                    context, dcontent
                )
            }

            is SyncUiState.Error -> ErrorState(state.message) {
                viewModel.onSyncButtonClicked(
                    context, dcontent
                )
            }

            else -> Unit // for other states
        }
    }
}


@Composable
private fun IdleState(onSyncClicked: () -> Unit) {
    Button(onClick = onSyncClicked) {
        Text("Sync Photos")
    }
}

@Composable
private fun LoadingState(uploaded: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Uploading photos...")
        Text("$uploaded out of $total uploaded")
    }
}

@Composable
private fun SuccessState(count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green)
        Text("Synced $count photos!")
    }
}

@Composable
private fun PermissionDeniedState(onRetryClicked: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Permission denied. Please grant storage access.")
        Button(onClick = onRetryClicked) {
            Text("Try Again")
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetryClicked: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Error: $message")
        Button(onClick = onRetryClicked) {
            Text("Retry")
        }
    }
}