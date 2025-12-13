package com.brainburst.presentation.navigation

sealed class Screen {
    data object Splash : Screen()
    data object Auth : Screen()
    data object Home : Screen()
    data object Sudoku : Screen()
    data object Leaderboard : Screen()
}

