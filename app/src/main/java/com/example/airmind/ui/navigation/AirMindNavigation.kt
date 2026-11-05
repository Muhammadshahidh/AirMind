package com.example.airmind.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.airmind.core.di.AppContainer
import com.example.airmind.ui.screens.AboutApplicationScreen
import com.example.airmind.ui.screens.AiAssistantScreen
import com.example.airmind.ui.screens.BluetoothManagerScreen
import com.example.airmind.ui.screens.DeviceScannerScreen
import com.example.airmind.ui.screens.EarbudsControlScreen
import com.example.airmind.ui.screens.GestureSettingsScreen
import com.example.airmind.ui.screens.LandingScreen
import com.example.airmind.ui.screens.MainDashboardScreen
import com.example.airmind.ui.screens.PrivacyDashboardScreen
import com.example.airmind.ui.screens.RemoteListeningScreen
import com.example.airmind.ui.screens.SettingsScreen
import com.example.airmind.ui.screens.SmartAutomationScreen
import com.example.airmind.ui.screens.SplashScreen
import com.example.airmind.ui.screens.WelcomeScreen

object AirMindRoutes {
    const val SPLASH = "splash"
    const val LANDING = "landing"
    const val WELCOME = "welcome"
    const val DEVICE_SCANNER = "device_scanner"
    const val DASHBOARD = "dashboard"
    const val EARBUDS_CONTROL = "earbuds_control"
    const val AI_ASSISTANT = "ai_assistant"
    const val REMOTE_LISTENING = "remote_listening"
    const val GESTURES = "gestures"
    const val BLUETOOTH_MANAGER = "bluetooth_manager"
    const val AUTOMATION = "automation"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}

@Composable
fun AirMindNavGraph(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AirMindRoutes.SPLASH,
        enterTransition = { fadeIn(animationSpec = tween(280)) },
        exitTransition = { fadeOut(animationSpec = tween(280)) },
        popEnterTransition = { fadeIn(animationSpec = tween(280)) },
        popExitTransition = { fadeOut(animationSpec = tween(280)) }
    ) {
        composable(AirMindRoutes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(AirMindRoutes.LANDING) {
                        popUpTo(AirMindRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AirMindRoutes.LANDING) {
            LandingScreen(
                onGetStarted = {
                    navController.navigate(AirMindRoutes.WELCOME)
                }
            )
        }

        composable(AirMindRoutes.WELCOME) {
            WelcomeScreen(
                onConnectEarbuds = {
                    navController.navigate(AirMindRoutes.DEVICE_SCANNER)
                },
                onSkipToDashboard = {
                    navController.navigate(AirMindRoutes.DASHBOARD) {
                        popUpTo(AirMindRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(AirMindRoutes.DEVICE_SCANNER) {
            DeviceScannerScreen(
                scanner = appContainer.bluetoothScanner,
                deviceManager = appContainer.bluetoothDeviceManager,
                onBackClick = { navController.popBackStack() },
                onDeviceSelected = {
                    navController.navigate(AirMindRoutes.DASHBOARD) {
                        popUpTo(AirMindRoutes.LANDING) { inclusive = true }
                    }
                }
            )
        }

        composable(AirMindRoutes.DASHBOARD) {
            MainDashboardScreen(
                deviceManager = appContainer.bluetoothDeviceManager,
                noiseController = appContainer.noiseCancellationController,
                sensorManager = appContainer.orientationSensorManager,
                onNavigateToConnect = { navController.navigate(AirMindRoutes.DEVICE_SCANNER) },
                onNavigateToEarbudsControl = { navController.navigate(AirMindRoutes.EARBUDS_CONTROL) },
                onNavigateToAssistant = { navController.navigate(AirMindRoutes.AI_ASSISTANT) },
                onNavigateToListening = { navController.navigate(AirMindRoutes.REMOTE_LISTENING) },
                onNavigateToGestures = { navController.navigate(AirMindRoutes.GESTURES) },
                onNavigateToBluetoothManager = { navController.navigate(AirMindRoutes.BLUETOOTH_MANAGER) },
                onNavigateToAutomation = { navController.navigate(AirMindRoutes.AUTOMATION) },
                onNavigateToSettings = { navController.navigate(AirMindRoutes.SETTINGS) }
            )
        }

        composable(AirMindRoutes.EARBUDS_CONTROL) {
            EarbudsControlScreen(
                actionHandler = appContainer.earbudActionHandler,
                noiseController = appContainer.noiseCancellationController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AirMindRoutes.AI_ASSISTANT) {
            AiAssistantScreen(
                assistantEngine = appContainer.aiAssistantEngine,
                speechManager = appContainer.speechManager,
                onBackClick = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(AirMindRoutes.DASHBOARD) {
                        popUpTo(AirMindRoutes.DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToListening = { navController.navigate(AirMindRoutes.REMOTE_LISTENING) },
                onNavigateToSettings = { navController.navigate(AirMindRoutes.SETTINGS) }
            )
        }

        composable(AirMindRoutes.REMOTE_LISTENING) {
            RemoteListeningScreen(
                listeningEngine = appContainer.remoteListeningEngine,
                preferencesRepository = appContainer.userPreferencesRepository,
                onBackClick = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(AirMindRoutes.DASHBOARD) {
                        popUpTo(AirMindRoutes.DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToAssistant = { navController.navigate(AirMindRoutes.AI_ASSISTANT) },
                onNavigateToSettings = { navController.navigate(AirMindRoutes.SETTINGS) }
            )
        }

        composable(AirMindRoutes.GESTURES) {
            GestureSettingsScreen(
                database = appContainer.database,
                actionHandler = appContainer.earbudActionHandler,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AirMindRoutes.BLUETOOTH_MANAGER) {
            BluetoothManagerScreen(
                deviceManager = appContainer.bluetoothDeviceManager,
                onBackClick = { navController.popBackStack() },
                onNavigateToPairing = { navController.navigate(AirMindRoutes.DEVICE_SCANNER) }
            )
        }

        composable(AirMindRoutes.AUTOMATION) {
            SmartAutomationScreen(
                database = appContainer.database,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AirMindRoutes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(AirMindRoutes.DASHBOARD) {
                        popUpTo(AirMindRoutes.DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToAssistant = { navController.navigate(AirMindRoutes.AI_ASSISTANT) },
                onNavigateToListening = { navController.navigate(AirMindRoutes.REMOTE_LISTENING) },
                onNavigateToAutomation = { navController.navigate(AirMindRoutes.AUTOMATION) },
                onNavigateToPrivacy = { navController.navigate(AirMindRoutes.PRIVACY) },
                onNavigateToAbout = { navController.navigate(AirMindRoutes.ABOUT) }
            )
        }

        composable(AirMindRoutes.PRIVACY) {
            PrivacyDashboardScreen(
                database = appContainer.database,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AirMindRoutes.ABOUT) {
            AboutApplicationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
