package com.brainburst.presentation.zip

import com.brainburst.domain.ads.AdManager
import com.brainburst.domain.game.GameRegistry
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.ZipMove
import com.brainburst.domain.game.zip.ZipDefinition
import com.brainburst.domain.game.zip.ZipPayload
import com.brainburst.domain.game.zip.ZipState
import com.brainburst.domain.game.zip.WallSide
import com.brainburst.domain.game.zip.ZipWall
import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.ResultDto
import com.brainburst.domain.model.toSerializable
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock

data class ZipUiState(
    val gridSize: Int = 6,
    val dots: List<ZipDotUi> = emptyList(),
    val walls: List<ZipWallUi> = emptyList(),
    val path: List<Position> = emptyList(),
    val lastConnectedDotIndex: Int = 0,
    val elapsedTimeFormatted: String = "00:00",
    val movesCount: Int = 0,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val hintPosition: Position? = null,
    val hintType: HintType = HintType.None,
    val isHintOnCooldown: Boolean = false,
    val hintCooldownProgress: Float = 0f,
    val showCompletionAnimation: Boolean = false
)

data class ZipDotUi(
    val position: Position,
    val index: Int
)

data class ZipWallUi(
    val row: Int,
    val col: Int,
    val side: WallSide
)

enum class HintType {
    None,
    NextCell,      // Highlight next valid move
    UndoSegment,   // Suggest backtracking
    NextDot        // Highlight next dot to reach
}

sealed class HintAction {
    data class HighlightCell(val position: Position) : HintAction()
    data class HighlightDot(val dotIndex: Int) : HintAction()
    data class SuggestUndo(val fromPosition: Position) : HintAction()
    object NoHint : HintAction()
}

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
    
    // Hint cooldown state
    private var hintCooldownJob: Job? = null

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
                        onSuccess = puzzleSuccess@{ puzzleDto ->
                            if (puzzleDto == null) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "No puzzle available. Please check back later."
                                )
                                return@puzzleSuccess
                            }

                            // If this is a different puzzle than before, clear old state
                            if (puzzleId != null && puzzleId != puzzleDto.puzzleId) {
                                gameStateRepository.clearGameState(puzzleId!!)
                            }

                            puzzleId = puzzleDto.puzzleId

                            // Decode payload
                            val definition = gameRegistry.get<ZipPayload, ZipState>(GameType.ZIP)
                            zipDefinition = definition as ZipDefinition
                            payload = definition.decodePayload(puzzleDto.payload)

                            // Try to restore saved state
                            val savedState = gameStateRepository.loadGameState(puzzleDto.puzzleId)

                            if (savedState != null) {
                                // Restore from saved state
                                currentState = ZipState(
                                    path = savedState.path.map { it.toPosition() },
                                    lastConnectedDotIndex = savedState.lastConnectedDotIndex,
                                    startedAtMillis = savedState.startedAtMillis,
                                    movesCount = savedState.movesCount,
                                    isCompleted = false
                                )
                                elapsedMillisWhenPaused = savedState.elapsedMillisAtPause
                            } else {
                                // Initialize fresh state
                                currentState = definition.initialState(payload!!)
                                elapsedMillisWhenPaused = 0L
                            }

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

        // Calculate elapsed time and add to paused total
        val now = Clock.System.now().toEpochMilliseconds()
        val sessionElapsed = now - timerStartedAtMillis
        elapsedMillisWhenPaused += sessionElapsed

        // Save state when pausing
        saveGameState()
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
        
        val wallsUi = payloadData.walls.map { wall ->
            ZipWallUi(
                row = wall.row,
                col = wall.col,
                side = wall.side
            )
        }

        _uiState.value = _uiState.value.copy(
            gridSize = payloadData.size,
            dots = dotsUi,
            walls = wallsUi,
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
        var newState = definition.applyMoveWithWalls(state, move, payloadData)
        
        // Update dot progress
        newState = definition.updateDotProgress(newState, payloadData)
        
        currentState = newState

        // Update UI
        updateUiFromState()

        // Check if puzzle is completed
        if (newState.isCompleted) {
            isDragging = false
            pauseTimer()
            
            // Show completion animation first
            _uiState.value = _uiState.value.copy(showCompletionAnimation = true)
            
            // Wait for animation, then submit
            viewModelScope.launch {
                delay(3000) // 3 seconds for animation
                _uiState.value = _uiState.value.copy(showCompletionAnimation = false)
                onSubmit()
            }
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
    
    /**
     * Find where user path diverges from solution
     */
    private fun findDivergence(userPath: List<Position>, solution: List<Position>): Int {
        for (i in userPath.indices) {
            if (i >= solution.size || userPath[i] != solution[i]) {
                return i
            }
        }
        return userPath.size  // No divergence yet
    }
    
    /**
     * Clear hint visualization
     */
    private fun clearHint() {
        _uiState.value = _uiState.value.copy(
            hintPosition = null,
            hintType = HintType.None
        )
    }
    
    /**
     * Determine what hint to show based on current state and solution
     */
    private fun determineHint(
        state: ZipState,
        solution: List<Position>,
        @Suppress("UNUSED_PARAMETER") payload: ZipPayload
    ): HintAction {
        // Edge case: puzzle complete
        if (state.isCompleted) return HintAction.NoHint
        
        // Edge case: empty path (should not happen, but handle it)
        if (state.path.isEmpty()) {
            return HintAction.HighlightDot(1)
        }
        
        // Find divergence point
        val divergenceIndex = findDivergence(state.path, solution)
        
        // User is on correct path - show next cell
        if (divergenceIndex == state.path.size) {
            val nextPos = solution.getOrNull(state.path.size)
            return if (nextPos != null) {
                HintAction.HighlightCell(nextPos)
            } else {
                HintAction.NoHint
            }
        }
        
        // User diverged - show first wrong cell for undo
        return HintAction.SuggestUndo(state.path[divergenceIndex])
    }

    fun onHintPress() {
        val state = currentState ?: return
        val payloadData = payload ?: return
        
        // Don't allow hint if on cooldown
        if (_uiState.value.isHintOnCooldown) return
        
        println("ZIP HINT: Starting hint calculation")
        println("ZIP HINT: Current path size: ${state.path.size}")
        
        // Pause timer during hint
        pauseTimer()
        
        viewModelScope.launch {
            // Show rewarded ad first
             adManager.showRewardedAd {
                viewModelScope.launch adCallback@{
                    // Use pre-calculated solution from payload
                    val solution = payloadData.solution.map { it.toPosition() }
                    
                    if (solution.isEmpty()) {
                        // No solution provided in payload
                        println("ZIP HINT: No solution found in payload!")
                        resumeTimer()
                        return@adCallback
                    }
                    
                    println("ZIP HINT: Solution found with ${solution.size} cells")
                    
                    // Determine what hint to show
                    val hintAction = determineHint(state, solution, payloadData)
                    
                    println("ZIP HINT: Hint action: $hintAction")
                    
                    when (hintAction) {
                        is HintAction.HighlightCell -> {
                            println("ZIP HINT: Highlighting cell at ${hintAction.position}")
                            _uiState.value = _uiState.value.copy(
                                hintPosition = hintAction.position,
                                hintType = HintType.NextCell
                            )
                            // Clear hint after 2.5 seconds
                            delay(2500)
                            clearHint()
                        }
                        is HintAction.SuggestUndo -> {
                            println("ZIP HINT: Suggesting undo at ${hintAction.fromPosition}")
                            _uiState.value = _uiState.value.copy(
                                hintPosition = hintAction.fromPosition,
                                hintType = HintType.UndoSegment
                            )
                            // Clear hint after 2.5 seconds
                            delay(2500)
                            clearHint()
                        }
                        is HintAction.HighlightDot -> {
                            println("ZIP HINT: Highlighting dot ${hintAction.dotIndex}")
                            _uiState.value = _uiState.value.copy(
                                hintPosition = payloadData.getDotPosition(hintAction.dotIndex),
                                hintType = HintType.NextDot
                            )
                            // Clear hint after 2.5 seconds
                            delay(2500)
                            clearHint()
                        }
                        HintAction.NoHint -> {
                            println("ZIP HINT: No hint available")
                            // Puzzle solved or ambiguous state - do nothing
                        }
                    }
                    
                    resumeTimer()
                    
                    // Start hint cooldown
                    startHintCooldown()
                }
            }
        }
    }
    
    private fun startHintCooldown() {
        // Cancel any existing cooldown
        hintCooldownJob?.cancel()
        
        // Set cooldown state
        _uiState.value = _uiState.value.copy(
            isHintOnCooldown = true,
            hintCooldownProgress = 0f
        )
        
        hintCooldownJob = viewModelScope.launch {
            val cooldownDuration = 5000L // 5 seconds
            val startTime = Clock.System.now().toEpochMilliseconds()
            
            while (true) {
                val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                val progress = (elapsed.toFloat() / cooldownDuration).coerceIn(0f, 1f)
                
                _uiState.value = _uiState.value.copy(hintCooldownProgress = progress)
                
                if (elapsed >= cooldownDuration) {
                    _uiState.value = _uiState.value.copy(
                        isHintOnCooldown = false,
                        hintCooldownProgress = 0f
                    )
                    break
                }
                
                delay(50) // Update ~20fps for smooth animation
            }
        }
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
                        puzzleId?.let { gameStateRepository.clearGameState(it) }
                        
                        // Record game completion for frequency capping
                        adManager.recordGameCompleted()
                        
                        // Show interstitial ad with frequency capping
                        if (adManager.shouldShowInterstitial()) {
                            adManager.showInterstitialAd {
                                adManager.recordInterstitialShown()
                                navigator.navigateTo(Screen.Leaderboard(GameType.ZIP))
                            }
                        } else {
                            // Skip ad and navigate directly
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

    /**
     * Called when the app goes to background (home button pressed, app switcher, etc.).
     * This pauses the timer and saves state immediately.
     */
    fun onAppPaused() {
        pauseTimer()
        // Force save state when app goes to background
        saveGameStateBlocking()
    }

    /**
     * Called when the app comes back to foreground.
     * This resumes the timer if the screen is still visible.
     */
    fun onAppResumed() {
        // Only resume if we're still on the Zip screen and timer was running
        // The screen visibility check ensures we don't resume if user navigated away
        if (currentState != null && puzzleId != null) {
            resumeTimer()
        }
    }

    /**
     * Called when the screen is stopped (onPause on Android, viewDidDisappear on iOS).
     * This saves the game state when the user leaves the screen.
     * Uses blocking save to ensure state is persisted even when the app is killed.
     */
    fun onScreenStopped() {
        pauseTimer() // This calls saveGameState() async
        // Also call blocking version to ensure save completes before process is killed
        saveGameStateBlocking()
    }

    private fun saveGameState() {
        val state = currentState ?: return
        val id = puzzleId ?: return

        viewModelScope.launch {
            // Calculate current total elapsed time
            val now = Clock.System.now().toEpochMilliseconds()
            val sessionElapsed = if (isTimerRunning) now - timerStartedAtMillis else 0L
            val totalElapsed = elapsedMillisWhenPaused + sessionElapsed

            val savedState = com.brainburst.domain.model.SavedGameState(
                puzzleId = id,
                gameType = GameType.ZIP,
                board = emptyList(), // ZIP doesn't use board
                fixedCells = emptyList(),
                startedAtMillis = state.startedAtMillis,
                movesCount = state.movesCount,
                elapsedMillisAtPause = totalElapsed,
                lastSavedAtMillis = now,
                path = state.path.toSerializable(),
                lastConnectedDotIndex = state.lastConnectedDotIndex
            )

            gameStateRepository.saveGameState(savedState)
        }
    }

    /**
     * Blocking version of saveGameState for use in lifecycle callbacks.
     * Uses runBlocking to ensure the save completes before the process is killed.
     * This is important on Android when the app is backgrounded or killed.
     */
    private fun saveGameStateBlocking() {
        val state = currentState ?: return
        val id = puzzleId ?: return

        runBlocking {
            try {
                withTimeout(1000) { // 1 second timeout to prevent hanging
                    // Calculate current total elapsed time
                    val now = Clock.System.now().toEpochMilliseconds()
                    val sessionElapsed = if (isTimerRunning) now - timerStartedAtMillis else 0L
                    val totalElapsed = elapsedMillisWhenPaused + sessionElapsed

                    val savedState = com.brainburst.domain.model.SavedGameState(
                        puzzleId = id,
                        gameType = GameType.ZIP,
                        board = emptyList(), // ZIP doesn't use board
                        fixedCells = emptyList(),
                        startedAtMillis = state.startedAtMillis,
                        movesCount = state.movesCount,
                        elapsedMillisAtPause = totalElapsed,
                        lastSavedAtMillis = now,
                        path = state.path.toSerializable(),
                        lastConnectedDotIndex = state.lastConnectedDotIndex
                    )

                    gameStateRepository.saveGameState(savedState)
                }
            } catch (e: Exception) {
                // Silently fail - if save doesn't complete, that's okay
                // The user's progress might be lost, but we don't want to crash
                // or hang the app shutdown process
            }
        }
    }
}
