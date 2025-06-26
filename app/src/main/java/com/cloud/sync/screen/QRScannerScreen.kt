package com.cloud.sync.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloud.sync.data.ui_state.ScanUiState
import com.cloud.sync.view_model.ScanViewModel
import com.journeyapps.barcodescanner.ScanContract

// ScanScreen.kt
@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    onNavigateToResult: (String?) -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Launchers setup
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        viewModel.handleScanResult(result.contents)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.handlePermissionResult(permissions)
    }

    // One-time launcher setup
    LaunchedEffect(Unit) {
        viewModel.setScanLauncher(scanLauncher)
        viewModel.setPermissionLauncher(permissionLauncher)
    }

    // Handle navigation when scanned
    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Scanned) {
            onNavigateToResult((uiState as ScanUiState.Scanned).content)
        }
    }

    ScanContent(
        modifier = modifier,
        uiState = uiState,
        onScanClicked = { viewModel.onScanButtonClicked(context) }
    )
}

@Composable
private fun ScanContent(
    modifier: Modifier,
    uiState: ScanUiState,
    onScanClicked: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ScanButton(onClick = onScanClicked)

        when (val state = uiState) {
            is ScanUiState.PermissionDenied -> PermissionDeniedMessage()
            is ScanUiState.Scanned -> ScannedContent(content = state.content)
            ScanUiState.Idle, ScanUiState.PermissionGranted -> {} // No UI for these states
        }
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Scan QR Code")
    }
}

@Composable
private fun PermissionDeniedMessage() {
    Text(
        text = "Permission denied. Please grant camera access.",
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun ScannedContent(content: String?) {
    Text(
        text = "Scanned: ${content ?: "Nothing"}",
        modifier = Modifier.padding(top = 16.dp)
    )
}