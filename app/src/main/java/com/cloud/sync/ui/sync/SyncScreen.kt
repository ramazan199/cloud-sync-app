package com.cloud.sync.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(syncViewModel: SyncViewModel = hiltViewModel()) {
    val uiState by syncViewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Gallery Sync", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(text = "Status:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = uiState.statusText,
                        modifier = Modifier.padding(top = 8.dp),
                        minLines = 2
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Sync New Photos Automatically",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Runs periodically in the background",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = uiState.isBackgroundSyncScheduled,
                    onCheckedChange = syncViewModel::onFromNowSyncToggled,
                    enabled = !uiState.isFullScanInProgress
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text("Manual Full Scan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Finds and uploads any photos missed in the past.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            val isFullScanning = uiState.isFullScanInProgress
            Button(
                onClick = { if (isFullScanning) syncViewModel.stopFullScan() else syncViewModel.startFullScan() },
                enabled = true, // Always allow starting/stopping the full scan
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFullScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                if (isFullScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Stop Full Scan")
                } else {
                    Text("Start Full Scan")
                }
            }
        }
    }
}