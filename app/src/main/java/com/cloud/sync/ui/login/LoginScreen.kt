package com.cloud.sync.ui.login


import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                coroutineScope.launch {
                    try {
                        viewModel.handleAuthResult(it)
                        onLoginSuccess()
                    } catch (e: AuthorizationException) {
                        Log.w("LoginScreen", "Authorization failed or was cancelled", e)
                        // TODO: Show a toast or snackbar message to the user
                    }
                }
            }
        } else {
            Log.w("LoginScreen", "Authorization flow was cancelled. Result code: ${result.resultCode}")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to Cloud Sync", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {
                val authIntent = viewModel.getAuthIntent()
                authLauncher.launch(authIntent)
            }) {
                Text("Sign In")
            }
        }
    }
}