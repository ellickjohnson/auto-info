package com.autoinfo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoinfo.ui.main.MainScreen
import com.autoinfo.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: com.autoinfo.ui.main.MainViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
