package com.monk.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.monk.app.data.datastore.PreferencesManager
import com.monk.app.ui.screens.HomeScreen
import com.monk.app.ui.screens.SettingsScreen
import com.monk.app.ui.screens.AppsScreen
import com.monk.app.ui.screens.ContactsScreen
import com.monk.app.ui.screens.OnboardingScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Apps : Screen("apps")
    object Contacts : Screen("contacts")
    // Privacy: No History screen - we don't store message history
}

@Composable
fun MonkNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    // Check if onboarding is completed
    val onboardingCompleted = remember {
        runBlocking { preferencesManager.onboardingCompleted.first() }
    }
    
    val startDestination = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    // Mark onboarding as completed
                    runBlocking { preferencesManager.setOnboardingCompleted(true) }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                onNavigateToOnboarding = { 
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
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

        composable(Screen.Contacts.route) {
            ContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
