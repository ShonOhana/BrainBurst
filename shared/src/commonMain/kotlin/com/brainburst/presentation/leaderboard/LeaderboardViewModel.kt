package com.brainburst.presentation.leaderboard

import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.ResultDto
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.domain.repository.PuzzleRepository
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val durationMs: Long,
    val formattedTime: String,
    val movesCount: Int,
    val isCurrentUser: Boolean = false
)

data class LeaderboardUiState(
    val gameType: GameType = GameType.MINI_SUDOKU_6X6,
    val date: String = "",
    val entries: List<LeaderboardEntry> = emptyList(),
    val currentUserEntry: LeaderboardEntry? = null,
    val currentUserRank: Int? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class LeaderboardViewModel(
    private val gameType: GameType,
    private val authRepository: AuthRepository,
    private val puzzleRepository: PuzzleRepository,
    private val firestore: FirebaseFirestore,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(LeaderboardUiState(gameType = gameType))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()
    
    init {
        loadLeaderboard()
    }
    
    private fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Get the latest available puzzle date (today's if exists, otherwise yesterday's)
            // This handles the case where it's before 8 UTC and today's puzzle hasn't been generated yet
            val latestDateResult = puzzleRepository.getLatestAvailablePuzzleDate(gameType)
            
            latestDateResult.fold(
                onSuccess = { latestDate ->
                    // If no puzzle exists at all, show appropriate message
                    if (latestDate == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            date = "",
                            errorMessage = "No puzzle available yet. Please check back later."
                        )
                        return@fold
                    }
                    
                    val puzzleId = "${gameType.name}_$latestDate"
                    val currentUserId = authRepository.currentUser.value?.uid
                    
                    // Load results from Firestore for the latest available puzzle
                    val result = puzzleRepository.getResultsForPuzzle(puzzleId, limit = 100)
                    
                    result.fold(
                        onSuccess = { results ->
                            if (results.isEmpty()) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    date = latestDate,
                                    errorMessage = "No one has completed this puzzle yet. Be the first!"
                                )
                                return@fold
                            }
                            
                            // Convert to leaderboard entries
                            val entries = results.mapIndexed { index, resultDto ->
                                LeaderboardEntry(
                                    rank = index + 1,
                                    userId = resultDto.userId,
                                    displayName = resultDto.displayName,
                                    durationMs = resultDto.durationMs,
                                    formattedTime = formatDuration(resultDto.durationMs),
                                    movesCount = resultDto.movesCount,
                                    isCurrentUser = resultDto.userId == currentUserId
                                )
                            }
                            
                            // Find current user's entry
                            val currentUserEntry = entries.find { it.isCurrentUser }
                            val currentUserRank = currentUserEntry?.rank
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                date = latestDate,
                                entries = entries,
                                currentUserEntry = currentUserEntry,
                                currentUserRank = currentUserRank
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                date = latestDate,
                                errorMessage = error.message ?: "Failed to load leaderboard"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        date = "",
                        errorMessage = error.message ?: "Failed to load puzzle information"
                    )
                }
            )
        }
    }
    
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000.0  // Convert to double for decimal precision
        val minutes = (totalSeconds / 60).toInt()
        val seconds = totalSeconds % 60
        
        return if (minutes > 0) {
            // Format as "2:05.34" (minutes:seconds.milliseconds)
            buildString {
                append(minutes)
                append(":")
                append(String.format("%05.2f", seconds))
            }
        } else {
            // Format as "45.34s" (seconds.milliseconds)
            String.format("%.2fs", seconds)
        }
    }
    
    fun onBackPress() {
        navigator.navigateTo(Screen.Home)
    }
    
    fun onRetry() {
        loadLeaderboard()
    }
}

