package com.brainburst.presentation.navigation

sealed class Screen {
    data object Splash : Screen()
    data object Auth : Screen()
    data object Home : Screen()
    data object Settings : Screen()
    data object Sudoku : Screen()
    data class Leaderboard(val gameType: com.brainburst.domain.model.GameType) : Screen()
}


