package com.brainburst.presentation.home

import com.brainburst.domain.admin.AdminPuzzleUploader
import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.User
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.domain.repository.PuzzleRepository
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class HomeUiState(
    val user: User? = null,
    val games: List<GameStateUI> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val adminMessage: String? = null  // For admin upload feedback
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val puzzleRepository: PuzzleRepository,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope,
    private val adminPuzzleUploader: AdminPuzzleUploader
) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeAuthState()
        loadGameStates()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                
                // If user becomes null (logged out), navigate to auth
                if (user == null) {
                    navigator.navigateTo(Screen.Auth)
                } else {
                    // Reload game states when user changes
                    loadGameStates()
                }
            }
        }
    }
    
    private fun loadGameStates() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value ?: return@launch
            
            // Start with loading states
            _uiState.value = _uiState.value.copy(
                games = listOf(
                    GameStateUI.Loading(
                        gameType = GameType.MINI_SUDOKU_6X6,
                        title = "Mini Sudoku 6×6"
                    ),
                    GameStateUI.ComingSoon(
                        gameType = GameType.ZIP,
                        title = "Zip"
                    ),
                    GameStateUI.ComingSoon(
                        gameType = GameType.TANGO,
                        title = "Tango"
                    )
                )
            )
            
            // Load Sudoku state
            val sudokuState = loadSudokuState(user.uid)
            
            _uiState.value = _uiState.value.copy(
                games = listOf(
                    sudokuState,
                    GameStateUI.ComingSoon(
                        gameType = GameType.ZIP,
                        title = "Zip"
                    ),
                    GameStateUI.ComingSoon(
                        gameType = GameType.TANGO,
                        title = "Tango"
                    )
                )
            )
        }
    }
    
    private suspend fun loadSudokuState(userId: String): GameStateUI {
        // Get the latest available puzzle date (today's if exists, otherwise yesterday's)
        // Returns null if neither today nor yesterday has a puzzle
        val latestDate = puzzleRepository.getLatestAvailablePuzzleDate(GameType.MINI_SUDOKU_6X6)
            .getOrNull()
        
        // Check if today's puzzle exists
        val today = Clock.System.todayIn(TimeZone.UTC).toString()
        val hasTodayPuzzle = latestDate == today
        
        // Format the date for display - only if a puzzle exists (today or yesterday)
        // If neither exists, formattedDate will be empty and won't display
        val formattedDate = latestDate?.let { DateFormatter.formatPuzzleDate(it) } ?: ""
        
        // Check if user has completed the latest available puzzle
        // This will check yesterday's puzzle if today's doesn't exist yet (before 9 AM UTC)
        val hasCompleted = if (latestDate != null) {
            puzzleRepository.hasUserCompletedToday(
                userId = userId,
                gameType = GameType.MINI_SUDOKU_6X6
            ).getOrElse { false }
        } else {
            false // If no puzzle exists, user can't have completed it
        }
        
        return if (hasCompleted) {
            // TODO: Load actual completion time from results
            GameStateUI.Completed(
                gameType = GameType.MINI_SUDOKU_6X6,
                title = "Mini Sudoku 6×6",
                subtitle = "Daily 6×6 Sudoku challenge",
                completionTimeFormatted = "--:--",
                formattedDate = formattedDate
            )
        } else {
            GameStateUI.Available(
                gameType = GameType.MINI_SUDOKU_6X6,
                title = "Mini Sudoku 6×6",
                subtitle = "Daily 6×6 Sudoku challenge",
                formattedDate = formattedDate,
                hasTodayPuzzle = hasTodayPuzzle
            )
        }
    }
    
    fun onGameClick(gameType: GameType) {
        when (gameType) {
            GameType.MINI_SUDOKU_6X6 -> {
                // Check game state
                val gameState = _uiState.value.games.find { it.gameType == gameType }
                when (gameState) {
                    is GameStateUI.Available -> {
                        // If today's puzzle doesn't exist, navigate to leaderboard instead of game
                        if (gameState.hasTodayPuzzle) {
                            // Navigate to play the game
                            navigator.navigateTo(Screen.Sudoku)
                        } else {
                            // Navigate to leaderboard to see yesterday's results
                            navigator.navigateTo(Screen.Leaderboard(gameType))
                        }
                    }
                    is GameStateUI.Completed -> {
                        // Navigate to leaderboard to see results
                        navigator.navigateTo(Screen.Leaderboard(gameType))
                    }
                    else -> {
                        // Loading or Coming Soon - do nothing
                    }
                }
            }
            else -> {
                // Coming soon - do nothing or show toast
            }
        }
    }
    
    fun onLogoutClick() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = authRepository.signOut()
            
            result.fold(
                onSuccess = {
                    // Navigation will be handled by observeAuthState when user becomes null
                    _uiState.value = HomeUiState()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Logout failed"
                    )
                }
            )
        }
    }
    
    /**
     * ADMIN FUNCTION: Upload today's test puzzle
     * Call this once to add a test puzzle for development
     */
    fun uploadTestPuzzle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(adminMessage = "Uploading test puzzle...")
            
            val result = adminPuzzleUploader.uploadTodayTestPuzzle()
            
            result.fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(adminMessage = message)
                    // Reload game states to show the new puzzle
                    loadGameStates()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        adminMessage = "❌ Upload failed: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun clearAdminMessage() {
        _uiState.value = _uiState.value.copy(adminMessage = null)
    }
}


