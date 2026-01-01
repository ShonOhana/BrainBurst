package com.brainburst.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Navigator {
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }
    
    fun navigateBack() {
        // For MVP, we'll implement simple back navigation
        when (_currentScreen.value) {
            is Screen.Auth -> navigateTo(Screen.Splash)
            is Screen.Home -> navigateTo(Screen.Auth)
            is Screen.Sudoku -> navigateTo(Screen.Home)
            is Screen.Leaderboard -> navigateTo(Screen.Home)
            else -> {} // Can't go back from Splash
        }
    }
}




