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
                    
                    // Get unique user IDs
                    val userIds = results.map { it.userId }.distinct()
                    
                    // Fetch user data from Firestore
                    val userDisplayNames = fetchUserDisplayNames(userIds)
                    
                    // Convert to leaderboard entries
                    val entries = results.mapIndexed { index, resultDto ->
                        LeaderboardEntry(
                            rank = index + 1,
                            userId = resultDto.userId,
                            displayName = userDisplayNames[resultDto.userId] 
                                ?: "Player ${resultDto.userId.take(6)}",
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
    
    private suspend fun fetchUserDisplayNames(userIds: List<String>): Map<String, String> {
        return try {
            val usersCollection = firestore.collection("users")
            val userMap = mutableMapOf<String, String>()
            
            // Fetch each user document
            userIds.forEach { userId ->
                try {
                    val userDoc = usersCollection.document(userId).get()
                    if (userDoc.exists) {
                        val firstName = userDoc.get<String?>("firstName") ?: ""
                        val lastName = userDoc.get<String?>("lastName") ?: ""
                        val displayName = userDoc.get<String?>("displayName")
                        val email = userDoc.get<String?>("email")
                        
                        // Prefer firstName + lastName, then displayName, then email, then fallback
                        val fullName = when {
                            firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
                            firstName.isNotBlank() -> firstName
                            lastName.isNotBlank() -> lastName
                            !displayName.isNullOrBlank() -> displayName
                            !email.isNullOrBlank() -> email
                            else -> "Player ${userId.take(6)}"
                        }
                        userMap[userId] = fullName
                    } else {
                        userMap[userId] = "Player ${userId.take(6)}"
                    }
                } catch (e: Exception) {
                    // If fetch fails, use fallback
                    userMap[userId] = "Player ${userId.take(6)}"
                }
            }
            
            userMap
        } catch (e: Exception) {
            // If all fetches fail, return empty map (will use fallback in entries)
            emptyMap()
        }
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

