package com.scriptmanager.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.scriptmanager.app.ui.screens.*
import com.scriptmanager.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ScriptManagerApp

        setContent {
            val keepScreenOn by app.preferencesManager.keepScreenOn
                .collectAsState(initial = true)

            // Keep screen on setting
            if (keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            AppTheme(context = this) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "script_list"
                ) {
                    // Script list
                    composable("script_list") {
                        ScriptListScreen(
                            repository = app.scriptRepository,
                            onCreateScript = {
                                navController.navigate("script_edit/new")
                            },
                            onEditScript = { id ->
                                navController.navigate("script_edit/$id")
                            },
                            onRunScript = { id ->
                                navController.navigate("script_run/$id")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    // Script edit
                    composable(
                        route = "script_edit/{scriptId}",
                        arguments = listOf(
                            navArgument("scriptId") {
                                type = NavType.StringType
                                defaultValue = "new"
                            }
                        )
                    ) { backStackEntry ->
                        val scriptId = backStackEntry.arguments?.getString("scriptId")
                        ScriptEditScreen(
                            scriptId = if (scriptId == "new") null else scriptId,
                            repository = app.scriptRepository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Script run
                    composable(
                        route = "script_run/{scriptId}",
                        arguments = listOf(
                            navArgument("scriptId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val scriptId = backStackEntry.arguments?.getString("scriptId")
                            ?: return@composable
                        ScriptRunScreen(
                            scriptId = scriptId,
                            repository = app.scriptRepository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Settings
                    composable("settings") {
                        SettingsScreen(
                            preferencesManager = app.preferencesManager,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
