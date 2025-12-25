package com.brainburst.presentation.leaderboard

import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.ResultDto
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
            
            val today = Clock.System.todayIn(TimeZone.UTC).toString()
            val puzzleId = "${gameType.name}_$today"
            val currentUserId = authRepository.currentUser.value?.uid
            
            // Load results from Firestore
            val result = puzzleRepository.getResultsForPuzzle(puzzleId, limit = 100)
            
            result.fold(
                onSuccess = { results ->
                    if (results.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            date = today,
                            errorMessage = "No one has completed today's puzzle yet. Be the first!"
                        )
                        return@fold
                    }
                    
                    // Convert to leaderboard entries
                    val entries = results.mapIndexed { index, resultDto ->
                        LeaderboardEntry(
                            rank = index + 1,
                            userId = resultDto.userId,
                            displayName = getUserDisplayName(resultDto.userId),
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
                        date = today,
                        entries = entries,
                        currentUserEntry = currentUserEntry,
                        currentUserRank = currentUserRank
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        date = today,
                        errorMessage = error.message ?: "Failed to load leaderboard"
                    )
                }
            )
        }
    }
    
    private fun getUserDisplayName(userId: String): String {
        // TODO: Fetch from users collection in Firestore
        // For now, show anonymized names
        return "Player ${userId.take(6)}"
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        // Use buildString for Kotlin Native compatibility (iOS)
        return buildString {
            append(minutes)
            append(":")
            append(if (secs < 10) "0$secs" else "$secs")
        }
    }
    
    fun onBackPress() {
        navigator.navigateTo(Screen.Home)
    }
    
    fun onRetry() {
        loadLeaderboard()
    }
}

