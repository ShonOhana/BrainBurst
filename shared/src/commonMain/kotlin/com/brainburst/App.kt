package com.brainburst

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.brainburst.di.getAllModules
import com.brainburst.domain.ads.AdManager
import com.brainburst.domain.notifications.NotificationManager
import com.brainburst.domain.repository.PreferencesRepository
import com.brainburst.presentation.auth.AuthScreen
import com.brainburst.presentation.auth.AuthViewModel
import com.brainburst.presentation.home.HomeScreen
import com.brainburst.presentation.home.HomeViewModel
import com.brainburst.presentation.leaderboard.LeaderboardScreen
import com.brainburst.presentation.leaderboard.LeaderboardViewModel
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import com.brainburst.presentation.settings.SettingsScreen
import com.brainburst.presentation.settings.SettingsViewModel
import com.brainburst.presentation.splash.SplashScreen
import com.brainburst.presentation.splash.SplashViewModel
import com.brainburst.presentation.sudoku.SudokuScreen
import com.brainburst.presentation.sudoku.SudokuViewModel
import com.brainburst.presentation.zip.ZipScreen
import com.brainburst.presentation.zip.ZipViewModel
import com.brainburst.presentation.tango.TangoScreen
import com.brainburst.presentation.tango.TangoViewModel
import com.brainburst.platform.PlatformBackHandler
import org.koin.core.parameter.parametersOf
import com.brainburst.ui.theme.BrainBurstTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun App(koinModules: List<org.koin.core.module.Module> = getAllModules()) {
    KoinApplication(application = {
        modules(koinModules)
    }) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BrainBurstTheme {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    val navigator: Navigator = koinInject()
    val currentScreen by navigator.currentScreen.collectAsState()
    
    // Preload ads when app starts
    val adManager: AdManager = koinInject()
    LaunchedEffect(Unit) {
        adManager.preloadInterstitialAd()  // For leaderboard (short, 5-15s)
        adManager.preloadRewardedAd()      // For hints (long, 30s, higher revenue)
    }
    
    // Initialize notifications if enabled
    val notificationManager: NotificationManager = koinInject()
    val preferencesRepository: PreferencesRepository = koinInject()
    LaunchedEffect(Unit) {
        try {
            preferencesRepository.getNotificationsEnabled().collect { notificationsEnabled ->
                if (notificationsEnabled) {
                    notificationManager.scheduleDailyNotifications()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    when (currentScreen) {
        is Screen.Splash -> {
            val viewModel: SplashViewModel = koinInject()
            SplashScreen(viewModel)
        }
        is Screen.Auth -> {
            val viewModel: AuthViewModel = koinInject()
            AuthScreen(viewModel)
        }
        is Screen.Home -> {
            val viewModel: HomeViewModel = koinInject()
            HomeScreen(viewModel)
        }
        is Screen.Settings -> {
            val viewModel: SettingsViewModel = koinInject()
            // Handle native back button - same as toolbar back arrow
            PlatformBackHandler(enabled = true) {
                viewModel.onBackClick()
            }
            SettingsScreen(viewModel)
        }
        is Screen.Sudoku -> {
            val viewModel: SudokuViewModel = koinInject()
            // Handle native back button - same as toolbar back arrow
            PlatformBackHandler(enabled = true) {
                viewModel.onBackPress()
            }
            SudokuScreen(viewModel, adManager)
        }
        is Screen.Zip -> {
            val viewModel: ZipViewModel = koinInject()
            // Handle native back button - same as toolbar back arrow
            PlatformBackHandler(enabled = true) {
                viewModel.onBackPress()
            }
            ZipScreen(viewModel, adManager)
        }
        is Screen.Tango -> {
            val viewModel: TangoViewModel = koinInject()
            // Handle native back button - same as toolbar back arrow
            PlatformBackHandler(enabled = true) {
                viewModel.onBackClick()
            }
            TangoScreen(viewModel)
        }
        is Screen.Leaderboard -> {
            val leaderboardScreen = currentScreen as Screen.Leaderboard
            val viewModel: LeaderboardViewModel = koinInject { parametersOf(leaderboardScreen.gameType) }
            // Handle native back button - same as toolbar back arrow
            PlatformBackHandler(enabled = true) {
                viewModel.onBackPress()
            }
            LeaderboardScreen(viewModel, adManager)
        }
    }
}


