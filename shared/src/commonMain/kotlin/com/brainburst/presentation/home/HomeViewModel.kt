package com.brainburst.presentation.home

import com.brainburst.domain.admin.AdminPuzzleUploader
import com.brainburst.domain.ads.AdManager
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
    private val adminPuzzleUploader: AdminPuzzleUploader,
    private val adManager: AdManager
) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    // Cache to avoid unnecessary network calls
    private var lastCheckedDate: String? = null
    private var cachedSudokuState: GameStateUI? = null
    private var isDataLoaded = false
    
    init {
        observeAuthState()
        observeNavigation()
        loadGameStates()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
                
                // If user becomes null (logged out), navigate to auth
                if (user == null) {
                    navigator.navigateTo(Screen.Auth)
                    // Clear cache on logout
                    lastCheckedDate = null
                    cachedSudokuState = null
                    isDataLoaded = false
                } else {
                    // Reload game states when user changes (force refresh for new user)
                    loadGameStates(forceRefresh = true)
                }
            }
        }
    }
    
    private fun loadGameStates(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val user = authRepository.currentUser.value ?: return@launch
            
            // Check if we need to reload based on date change
            val today = Clock.System.todayIn(TimeZone.UTC).toString()
            val shouldReload = forceRefresh || 
                !isDataLoaded || 
                lastCheckedDate != today ||
                cachedSudokuState == null
            
            // If we have cached data and date hasn't changed, use cache
            if (!shouldReload && cachedSudokuState != null) {
                val zipState = loadZipState(user.uid)
                _uiState.value = _uiState.value.copy(
                    games = listOf(
                        cachedSudokuState!!,
                        zipState,
                        GameStateUI.ComingSoon(
                            gameType = GameType.TANGO,
                            title = "Tango"
                        )
                    )
                )
                return@launch
            }
            
            // Start with loading states only if we don't have cached data
            if (cachedSudokuState == null) {
                _uiState.value = _uiState.value.copy(
                    games = listOf(
                        GameStateUI.Loading(
                            gameType = GameType.MINI_SUDOKU_6X6,
                            title = "Mini Sudoku 6×6"
                        ),
                        GameStateUI.Loading(
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
            
            // Load Sudoku state
            val sudokuState = loadSudokuState(user.uid)
            
            // Load ZIP state
            val zipState = loadZipState(user.uid)
            
            // Cache the result
            cachedSudokuState = sudokuState
            lastCheckedDate = today
            isDataLoaded = true
            
            _uiState.value = _uiState.value.copy(
                games = listOf(
                    sudokuState,
                    zipState,
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
    
    private suspend fun loadZipState(userId: String): GameStateUI {
        val latestDate = puzzleRepository.getLatestAvailablePuzzleDate(GameType.ZIP)
            .getOrNull()
        
        val today = Clock.System.todayIn(TimeZone.UTC).toString()
        val hasTodayPuzzle = latestDate == today
        
        val formattedDate = latestDate?.let { DateFormatter.formatPuzzleDate(it) } ?: ""
        
        val hasCompleted = if (latestDate != null) {
            puzzleRepository.hasUserCompletedToday(
                userId = userId,
                gameType = GameType.ZIP
            ).getOrElse { false }
        } else {
            false
        }
        
        return if (hasCompleted) {
            GameStateUI.Completed(
                gameType = GameType.ZIP,
                title = "Zip",
                subtitle = "Connect the dots",
                completionTimeFormatted = "--:--",
                formattedDate = formattedDate
            )
        } else {
            GameStateUI.Available(
                gameType = GameType.ZIP,
                title = "Zip",
                subtitle = "Connect the dots",
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
                        // User hasn't completed the puzzle - show ad then navigate
                        viewModelScope.launch {
//                            adManager.showInterstitialAd {
                                navigator.navigateTo(Screen.Sudoku)
//                            }
                        }
                    }
                    is GameStateUI.Completed -> {
                        // User has completed - navigate directly to results (no ad interruption)
                        viewModelScope.launch {
                            navigator.navigateTo(Screen.Leaderboard(gameType))
                        }
                    }
                    else -> {
                        // Loading or Coming Soon - do nothing
                    }
                }
            }
            GameType.ZIP -> {
                val gameState = _uiState.value.games.find { it.gameType == gameType }
                when (gameState) {
                    is GameStateUI.Available -> {
                        viewModelScope.launch {
                            navigator.navigateTo(Screen.Zip)
                        }
                    }
                    is GameStateUI.Completed -> {
                        // User has completed - navigate directly to results (no ad interruption)
                        viewModelScope.launch {
                            navigator.navigateTo(Screen.Leaderboard(gameType))
                        }
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
    
    fun onSettingsClick() {
        navigator.navigateTo(Screen.Settings)
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
                    // Reload game states to show the new puzzle (force refresh)
                    loadGameStates(forceRefresh = true)
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
    
    /**
     * Refresh game states - useful when returning from game screen after completion
     */
    fun refreshGameStates() {
        loadGameStates(forceRefresh = true)
    }
    
    /**
     * Observe navigation to refresh when returning to Home from game screens
     * This ensures completion status is updated when user returns from playing a game
     */
    private fun observeNavigation() {
        viewModelScope.launch {
            var previousScreen: Screen? = null
            navigator.currentScreen.collect { currentScreen ->
                // When navigating to Home from Sudoku or Leaderboard, refresh to get updated completion status
                if (currentScreen is Screen.Home && previousScreen != null) {
                    val cameFromGameScreen = previousScreen is Screen.Sudoku || previousScreen is Screen.Zip || previousScreen is Screen.Leaderboard
                    if (cameFromGameScreen && isDataLoaded) {
                        // Only refresh if we already have data loaded (to avoid double loading on initial load)
                        // This ensures we get the latest completion status when returning from game
                        loadGameStates(forceRefresh = true)
                    }
                }
                previousScreen = currentScreen
            }
        }
    }
}


