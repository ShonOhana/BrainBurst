package com.brainburst.presentation.zip

import com.brainburst.domain.ads.AdManager
import com.brainburst.domain.game.GameRegistry
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.ZipMove
import com.brainburst.domain.game.zip.ZipDefinition
import com.brainburst.domain.game.zip.ZipPayload
import com.brainburst.domain.game.zip.ZipState
import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.ResultDto
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.domain.repository.GameStateRepository
import com.brainburst.domain.repository.PuzzleRepository
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class ZipUiState(
    val gridSize: Int = 6,
    val dots: List<ZipDotUi> = emptyList(),
    val path: List<Position> = emptyList(),
    val lastConnectedDotIndex: Int = 0,
    val elapsedTimeFormatted: String = "00:00",
    val movesCount: Int = 0,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

data class ZipDotUi(
    val position: Position,
    val index: Int
)

sealed class ZipEvent {
    data class PuzzleCompleted(
        val durationMs: Long,
        val movesCount: Int
    ) : ZipEvent()

    data class ValidationError(val message: String) : ZipEvent()
}

class ZipViewModel(
    private val gameRegistry: GameRegistry,
    private val puzzleRepository: PuzzleRepository,
    private val authRepository: AuthRepository,
    private val gameStateRepository: GameStateRepository,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope,
    private val adManager: AdManager
) {
    private val _uiState = MutableStateFlow(ZipUiState())
    val uiState: StateFlow<ZipUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ZipEvent?>(null)
    val events: StateFlow<ZipEvent?> = _events.asStateFlow()

    private var zipDefinition: ZipDefinition? = null
    private var payload: ZipPayload? = null
    private var currentState: ZipState? = null
    private var timerJob: Job? = null
    private var puzzleId: String? = null

    // Timer state
    private var elapsedMillisWhenPaused: Long = 0L
    private var timerStartedAtMillis: Long = 0L
    private var isTimerRunning: Boolean = false

    init {
        loadPuzzle()
    }

    private fun loadPuzzle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val latestDateResult = puzzleRepository.getLatestAvailablePuzzleDate(GameType.ZIP)
            
            latestDateResult.fold(
                onSuccess = { latestDate ->
                    if (latestDate == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "No puzzle available. Please check back later."
                        )
                        return@launch
                    }
                    
                    val puzzleIdForDate = "${GameType.ZIP.name}_$latestDate"
                    val puzzleResult = puzzleRepository.getPuzzle(puzzleIdForDate)
                    
                    puzzleResult.fold(
                        onSuccess = { puzzleDto ->
                            if (puzzleDto == null) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "No puzzle available. Please check back later."
                                )
                                return@fold
                            }

                            puzzleId = puzzleDto.puzzleId

                            // Decode payload
                            val definition = gameRegistry.get<ZipPayload, ZipState>(GameType.ZIP)
                            zipDefinition = definition as ZipDefinition
                            payload = definition.decodePayload(puzzleDto.payload)

                            // Initialize state (no saved state for MVP)
                            currentState = definition.initialState(payload!!)
                            elapsedMillisWhenPaused = 0L

                            // Update UI
                            updateUiFromState()

                            // Start timer
                            resumeTimer()

                            _uiState.value = _uiState.value.copy(isLoading = false)
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to load puzzle"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load puzzle information"
                    )
                }
            )
        }
    }

    private fun resumeTimer() {
        if (isTimerRunning) return

        isTimerRunning = true
        timerStartedAtMillis = Clock.System.now().toEpochMilliseconds()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isTimerRunning) {
                delay(1000)
                updateTimer()
            }
        }
    }

    private fun pauseTimer() {
        if (!isTimerRunning) return

        isTimerRunning = false
        timerJob?.cancel()

        val now = Clock.System.now().toEpochMilliseconds()
        val sessionElapsed = now - timerStartedAtMillis
        elapsedMillisWhenPaused += sessionElapsed
    }

    private fun updateTimer() {
        if (timerStartedAtMillis == 0L && !isTimerRunning) {
            _uiState.value = _uiState.value.copy(elapsedTimeFormatted = "00:00")
            return
        }
        
        val now = Clock.System.now().toEpochMilliseconds()
        val sessionElapsed = if (isTimerRunning) now - timerStartedAtMillis else 0L
        val totalElapsed = elapsedMillisWhenPaused + sessionElapsed

        val elapsedSeconds = totalElapsed / 1000
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val formatted = buildString {
            append(if (minutes < 10) "0$minutes" else "$minutes")
            append(":")
            append(if (seconds < 10) "0$seconds" else "$seconds")
        }

        _uiState.value = _uiState.value.copy(elapsedTimeFormatted = formatted)
    }

    private fun updateUiFromState() {
        val state = currentState ?: return
        val payloadData = payload ?: return

        val dotsUi = payloadData.dots.map { dot ->
            ZipDotUi(
                position = dot.toPosition(),
                index = dot.index
            )
        }

        _uiState.value = _uiState.value.copy(
            gridSize = payloadData.size,
            dots = dotsUi,
            path = state.path,
            lastConnectedDotIndex = state.lastConnectedDotIndex,
            movesCount = state.movesCount,
            isCompleted = state.isCompleted
        )

        if (isTimerRunning || timerStartedAtMillis > 0L) {
            updateTimer()
        }
    }

    private var isDragging = false
    
    fun onDragStart(position: Position) {
        val state = currentState ?: return
        
        // Cannot drag if completed
        if (state.isCompleted) return
        
        isDragging = true
        
        // Backwards: only when dragging to the immediate previous cell (the only way they came from)
        val prevCell = state.path.getOrNull(state.path.size - 2)
        if (prevCell != null && position == prevCell) {
            removeLastCell()
        }
    }
    
    fun onDragMove(position: Position) {
        if (!isDragging) return
        
        val state = currentState ?: return
        val definition = zipDefinition ?: return
        val payloadData = payload ?: return
        
        // Cannot move if completed
        if (state.isCompleted) return
        
        // Backwards: only when dragging to the immediate previous cell (step back one, the way they came)
        val prevCell = state.path.getOrNull(state.path.size - 2)
        if (prevCell != null && position == prevCell) {
            removeLastCell()
            return
        }
        
        // Skip if this is already the last position
        if (state.lastPosition() == position) return
        
        // Apply move (forward)
        val move = ZipMove(position)
        var newState = definition.applyMove(state, move)
        
        // Update dot progress
        newState = definition.updateDotProgress(newState, payloadData)
        
        currentState = newState

        // Update UI
        updateUiFromState()

        // Check if puzzle is completed
        if (newState.isCompleted) {
            isDragging = false
            pauseTimer()
            onSubmit()
        }
    }
    
    fun onDragEnd() {
        isDragging = false
    }
    
    /**
     * Remove only the last cell from the path (step back the way they came).
     * Caller must have path.size > 1.
     */
    private fun removeLastCell() {
        val state = currentState ?: return
        val payloadData = payload ?: return
        
        if (state.path.size <= 1) return
        
        val newPath = state.path.dropLast(1)
        
        // Recalculate lastConnectedDotIndex
        var lastConnected = 0
        for (i in 1..payloadData.dotCount) {
            val dotPos = payloadData.getDotPosition(i)
            if (dotPos != null && newPath.contains(dotPos)) {
                lastConnected = i
            } else {
                break
            }
        }
        
        currentState = state.copy(
            path = newPath,
            lastConnectedDotIndex = lastConnected,
            movesCount = state.movesCount + 1,
            isCompleted = false
        )
        
        updateUiFromState()
    }

    fun onHintPress() {
        var state = currentState ?: return
        val payloadData = payload ?: return
        val definition = zipDefinition ?: return
        
        // Pause timer during hint
        pauseTimer()
        
        // Show rewarded ad
        viewModelScope.launch {
            adManager.showRewardedAd {
                // 1. REVERT: Remove last cell (undo wrong move) if path has more than dot 1
                if (state.path.size > 1) {
                    removeLastCell()
                    state = currentState ?: return@showRewardedAd
                }
                
                // 2. HINT: Apply correct next move
                val nextMove = findNextValidMove(state, payloadData)
                
                if (nextMove != null) {
                    var newState = definition.applyMove(state, ZipMove(nextMove))
                    newState = definition.updateDotProgress(newState, payloadData)
                    currentState = newState
                    updateUiFromState()
                    
                    if (newState.isCompleted) {
                        pauseTimer()
                        onSubmit()
                    } else {
                        resumeTimer()
                    }
                } else {
                    resumeTimer()
                }
            }
        }
    }
    
    private fun findNextValidMove(state: ZipState, payload: ZipPayload): Position? {
        val lastPos = state.lastPosition() ?: return null
        
        // Get all adjacent positions
        val adjacentPositions = listOf(
            Position(lastPos.row - 1, lastPos.col), // up
            Position(lastPos.row + 1, lastPos.col), // down
            Position(lastPos.row, lastPos.col - 1), // left
            Position(lastPos.row, lastPos.col + 1)  // right
        ).filter { pos ->
            // Within bounds
            pos.row in 0 until 6 && pos.col in 0 until 6
        }
        
        // Try to find a move that progresses toward next dot or fills board
        val nextDotIndex = state.lastConnectedDotIndex + 1
        if (nextDotIndex <= payload.dotCount) {
            val nextDotPos = payload.getDotPosition(nextDotIndex)
            if (nextDotPos != null) {
                // Prefer moves toward next dot
                val moveTowardDot = adjacentPositions.find { pos ->
                    !state.containsPosition(pos) && 
                    (kotlin.math.abs(pos.row - nextDotPos.row) + kotlin.math.abs(pos.col - nextDotPos.col) <
                     kotlin.math.abs(lastPos.row - nextDotPos.row) + kotlin.math.abs(lastPos.col - nextDotPos.col))
                }
                if (moveTowardDot != null) return moveTowardDot
            }
        }
        
        // Otherwise, any valid adjacent unvisited cell
        return adjacentPositions.find { !state.containsPosition(it) }
    }

    fun onResetPress() {
        val payloadData = payload ?: return
        val definition = zipDefinition ?: return

        // Reset to initial state but keep the original start time
        val startTime = currentState?.startedAtMillis ?: Clock.System.now().toEpochMilliseconds()
        currentState = definition.initialState(payloadData).copy(
            startedAtMillis = startTime,
            movesCount = (currentState?.movesCount ?: 0) + 1
        )

        updateUiFromState()
    }

    private fun onSubmit() {
        val state = currentState ?: return
        val definition = zipDefinition ?: return
        val payloadData = payload ?: return

        // Check if solution is correct
        if (definition.isCompleted(state, payloadData)) {
            val durationMs = elapsedMillisWhenPaused

            _uiState.value = _uiState.value.copy(isSubmitting = true)

            _events.value = ZipEvent.PuzzleCompleted(
                durationMs = durationMs,
                movesCount = state.movesCount
            )

            viewModelScope.launch {
                val result = submitResult(durationMs, state.movesCount)
                result.fold(
                    onSuccess = {
                        adManager.showInterstitialAd {
                            navigator.navigateTo(Screen.Leaderboard(GameType.ZIP))
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = "Failed to submit result: ${error.message ?: "Unknown error"}"
                        )
                    }
                )
            }
        }
    }

    private suspend fun submitResult(durationMs: Long, movesCount: Int): kotlin.Result<Unit> {
        val user = authRepository.currentUser.value 
            ?: return kotlin.Result.failure(Exception("User not authenticated"))
        
        val latestDate = puzzleRepository.getLatestAvailablePuzzleDate(GameType.ZIP).getOrNull()
            ?: return kotlin.Result.failure(Exception("No puzzle available"))
        
        val puzzleIdForDate = "${GameType.ZIP.name}_$latestDate"
        val puzzleDto = puzzleRepository.getPuzzle(puzzleIdForDate).getOrNull()
            ?: return kotlin.Result.failure(Exception("Puzzle not found"))

        val result = ResultDto(
            userId = user.uid,
            puzzleId = puzzleDto.puzzleId,
            gameType = GameType.ZIP,
            date = puzzleDto.date,
            durationMs = durationMs,
            movesCount = movesCount,
            displayName = user.displayName ?: "Anonymous"
        )

        return puzzleRepository.submitResult(result)
    }

    fun onBackPress() {
        pauseTimer()
        navigator.navigateTo(Screen.Home)
    }

    fun clearEvent() {
        _events.value = null
    }

    fun onCleared() {
        pauseTimer()
    }

    fun onScreenVisible() {
        resumeTimer()
    }

    fun onScreenHidden() {
        pauseTimer()
    }
}
