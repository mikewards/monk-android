package com.monk.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.monk.app.ui.screens.HomeScreen
import com.monk.app.ui.screens.SettingsScreen
import com.monk.app.ui.screens.AppsScreen
import com.monk.app.ui.screens.OnboardingScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Apps : Screen("apps")
    object Contacts : Screen("contacts")
    object History : Screen("history")
}

@Composable
fun MonkNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route // TODO: Check if onboarding completed
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToApps = { navController.navigate(Screen.Apps.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) }
            )
        }

        composable(Screen.Apps.route) {
            AppsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
