package com.cloud.sync
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cloud.sync.screen.AuthScreen
import com.cloud.sync.screen.ScanScreen
import com.cloud.sync.screen.SyncScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // 🔍 Check if we're in debug mode
    if (BuildConfig.DEBUG) {

        LaunchedEffect(Unit) {
            // Navigate directly to SyncScreen with dummy content
            navController.navigate("auth") {
                popUpTo(0) // Clear back stack
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "scan"
    ) {

        composable("scan") {
            ScanScreen(onNavigateToResult = { scannedContent ->
                navController.navigate("sync/$scannedContent")
            })
        }

        composable("auth"){
            AuthScreen(onAuthenticationSuccess = {
                // When authentication is successful, navigate to the SyncScreen
                navController.navigate("sync/Successfully_Authenticated") {
                    // This is important! It clears the navigation stack so the user
                    // can't press the back button to go back to the AuthScreen.
                    popUpTo("auth") {
                        inclusive = true
                    }
                }
            })
        }
        composable(
            route = "sync/{content}",
//            deepLinks = listOf(navDeepLink { uriPattern = "test://debug/sync-screen/{content}" }),
            arguments = listOf(navArgument("content") { type = NavType.StringType })
        ) { backStackEntry ->
            val content = backStackEntry.arguments?.getString("content")
            SyncScreen(content = content)
        }
    }
}