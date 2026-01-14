package com.brainburst.presentation.sudoku

import com.brainburst.domain.ads.AdManager
import com.brainburst.domain.game.GameRegistry
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.SudokuMove
import com.brainburst.domain.game.sudoku.Sudoku6x6Definition
import com.brainburst.domain.game.sudoku.Sudoku6x6Payload
import com.brainburst.domain.game.sudoku.SudokuState
import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.ResultDto
import com.brainburst.domain.model.SavedGameState
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

data class SudokuUiState(
    val board: List<List<Int>> = emptyList(),
    val fixedCells: Set<Position> = emptySet(),
    val selectedPosition: Position? = null,
    val invalidPositions: List<Position> = emptyList(),
    val elapsedTimeFormatted: String = "00:00",
    val movesCount: Int = 0,
    val isComplete: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed class SudokuEvent {
    data class PuzzleCompleted(
        val durationMs: Long,
        val movesCount: Int
    ) : SudokuEvent()

    data class ValidationError(val message: String) : SudokuEvent()
}

class SudokuViewModel(
    private val gameRegistry: GameRegistry,
    private val puzzleRepository: PuzzleRepository,
    private val authRepository: AuthRepository,
    private val gameStateRepository: GameStateRepository,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope,
    private val adManager: AdManager
) {
    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<SudokuEvent?>(null)
    val events: StateFlow<SudokuEvent?> = _events.asStateFlow()

    private var sudokuDefinition: Sudoku6x6Definition? = null
    private var payload: Sudoku6x6Payload? = null
    private var currentState: SudokuState? = null
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

            // Get the latest available puzzle date (today's if exists, otherwise yesterday's)
            // This handles the case where it's before 8 UTC and today's puzzle hasn't been generated yet
            val latestDateResult = puzzleRepository.getLatestAvailablePuzzleDate(GameType.MINI_SUDOKU_6X6)
            
            latestDateResult.fold(
                onSuccess = { latestDate ->
                    if (latestDate == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "No puzzle available. Please check back later."
                        )
                        return@launch
                    }
                    
                    // Construct puzzleId from the latest available date
                    val puzzleIdForDate = "${GameType.MINI_SUDOKU_6X6.name}_$latestDate"
                    
                    // Load the puzzle
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

                            // If this is a different puzzle than before, clear old state
                            if (puzzleId != null && puzzleId != puzzleDto.puzzleId) {
                                gameStateRepository.clearGameState(puzzleId!!)
                            }

                            puzzleId = puzzleDto.puzzleId

                            // Decode payload
                            val definition = gameRegistry.get<Sudoku6x6Payload, SudokuState>(GameType.MINI_SUDOKU_6X6)
                            sudokuDefinition = definition as Sudoku6x6Definition
                            payload = definition.decodePayload(puzzleDto.payload)

                            // Try to restore saved state
                            val savedState = gameStateRepository.loadGameState(puzzleDto.puzzleId)

                            if (savedState != null) {
                                // Restore from saved state
                                currentState = SudokuState(
                                    board = savedState.board,
                                    fixedCells = savedState.fixedCells.map { it.toPosition() }.toSet(),
                                    startedAtMillis = savedState.startedAtMillis,
                                    movesCount = savedState.movesCount
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
        // Only update if timer has been initialized
        if (timerStartedAtMillis == 0L && !isTimerRunning) {
            // Timer hasn't started yet, show 00:00
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
        val definition = sudokuDefinition ?: return
        val payloadData = payload ?: return

        // Validate current state
        val validation = definition.validateState(state, payloadData)

        _uiState.value = _uiState.value.copy(
            board = state.board,
            fixedCells = state.fixedCells,
            invalidPositions = validation.invalidPositions,
            movesCount = state.movesCount,
            isComplete = state.isComplete()
        )

        // Update timer display only if timer has been started
        if (isTimerRunning || timerStartedAtMillis > 0L) {
            updateTimer()
        }
    }

    fun onCellClick(position: Position) {
        // Cannot select fixed cells
        if (position in (_uiState.value.fixedCells)) {
            return
        }

        _uiState.value = _uiState.value.copy(selectedPosition = position)
    }

    fun onNumberPress(number: Int) {
        val position = _uiState.value.selectedPosition ?: return
        val state = currentState ?: return
        val definition = sudokuDefinition ?: return

        // Apply move
        val move = SudokuMove(position, number)
        currentState = definition.applyMove(state, move)

        // Update UI
        updateUiFromState()

        // Check if puzzle just became complete and stop timer immediately
        if (currentState?.isComplete() == true) {
            pauseTimer()
        }

        // Save state after move
        saveGameState()
    }

    fun onErasePress() {
        val position = _uiState.value.selectedPosition ?: return
        onNumberPress(0) // 0 means erase
    }

    fun onHintPress() {
        val state = currentState ?: return
        val payloadData = payload ?: return
        
        // Pause the timer
        pauseTimer()
        
        // Find all empty cells that are not fixed
        val emptyCells = mutableListOf<Position>()
        for (row in state.board.indices) {
            for (col in state.board[row].indices) {
                val position = Position(row, col)
                if (state.board[row][col] == 0 && position !in state.fixedCells) {
                    emptyCells.add(position)
                }
            }
        }
        
        // If no empty cells, nothing to hint
        if (emptyCells.isEmpty()) {
            resumeTimer()
            return
        }
        
        // Show rewarded ad (30 seconds, higher revenue)
        viewModelScope.launch {
            adManager.showRewardedAd {
                // After user watches full ad and earns reward, give hint
                val randomCell = emptyCells.random()
                val correctValue = payloadData.solutionBoard[randomCell.row][randomCell.col]
                
                // Apply the correct value
                val move = SudokuMove(randomCell, correctValue)
                currentState = sudokuDefinition?.applyMove(state, move)
                
                // Update UI
                updateUiFromState()
                
                // Check if puzzle just became complete and stop timer immediately
                if (currentState?.isComplete() == true) {
                    pauseTimer()
                } else {
                    // Resume the timer only if puzzle is not complete
                    resumeTimer()
                }
                
                // Save state
                saveGameState()
            }
        }
    }

    fun onSubmit() {
        val state = currentState ?: return
        val definition = sudokuDefinition ?: return
        val payloadData = payload ?: return

        // Check if board is complete
        if (!state.isComplete()) {
            _events.value = SudokuEvent.ValidationError("Please fill all cells before submitting")
            return
        }

        // Check if solution is correct
        if (definition.isCompleted(state, payloadData)) {
            // Timer already stopped when puzzle became complete
            
            // Calculate total duration
            val durationMs = elapsedMillisWhenPaused

            // Set submitting state
            _uiState.value = _uiState.value.copy(isSubmitting = true)

            // Emit completion event
            _events.value = SudokuEvent.PuzzleCompleted(
                durationMs = durationMs,
                movesCount = state.movesCount
            )
            // Clear saved state
            viewModelScope.launch {
                // Submit result to Firestore
                val result = submitResult(durationMs, state.movesCount)
                result.fold(
                    onSuccess = {
                        puzzleId?.let { gameStateRepository.clearGameState(it) }
                        // Show ad before navigating to leaderboard
                        adManager.showInterstitialAd {
                            navigator.navigateTo(Screen.Leaderboard(GameType.MINI_SUDOKU_6X6))
                        }
                    },
                    onFailure = { error ->
                        // If submission fails, hide loader and show error
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = "Failed to submit result: ${error.message ?: "Unknown error"}"
                        )
                    }
                )
            }
        } else {
            _events.value = SudokuEvent.ValidationError("Solution is incorrect. Please check your answers.")
        }
    }

    private fun saveGameState() {
        val state = currentState ?: return
        val id = puzzleId ?: return

        viewModelScope.launch {
            // Calculate current total elapsed time
            val now = Clock.System.now().toEpochMilliseconds()
            val sessionElapsed = if (isTimerRunning) now - timerStartedAtMillis else 0L
            val totalElapsed = elapsedMillisWhenPaused + sessionElapsed

            val savedState = SavedGameState(
                puzzleId = id,
                gameType = GameType.MINI_SUDOKU_6X6,
                board = state.board,
                fixedCells = state.fixedCells.toSerializable(),
                startedAtMillis = state.startedAtMillis,
                movesCount = state.movesCount,
                elapsedMillisAtPause = totalElapsed,
                lastSavedAtMillis = now
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

                    val savedState = SavedGameState(
                        puzzleId = id,
                        gameType = GameType.MINI_SUDOKU_6X6,
                        board = state.board,
                        fixedCells = state.fixedCells.toSerializable(),
                        startedAtMillis = state.startedAtMillis,
                        movesCount = state.movesCount,
                        elapsedMillisAtPause = totalElapsed,
                        lastSavedAtMillis = now
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

    private suspend fun submitResult(durationMs: Long, movesCount: Int): kotlin.Result<Unit> {
        val user = authRepository.currentUser.value ?: return kotlin.Result.failure(Exception("User not authenticated"))
        
        // Get the latest available puzzle date (today's if exists, otherwise yesterday's)
        val latestDate = puzzleRepository.getLatestAvailablePuzzleDate(GameType.MINI_SUDOKU_6X6).getOrNull()
            ?: return kotlin.Result.failure(Exception("No puzzle available"))
        
        // Construct puzzleId and get the puzzle
        val puzzleIdForDate = "${GameType.MINI_SUDOKU_6X6.name}_$latestDate"
        val puzzleDto = puzzleRepository.getPuzzle(puzzleIdForDate).getOrNull()
            ?: return kotlin.Result.failure(Exception("Puzzle not found"))

        val result = ResultDto(
            userId = user.uid,
            puzzleId = puzzleDto.puzzleId,
            gameType = GameType.MINI_SUDOKU_6X6,
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

    // Lifecycle methods for screen visibility
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
        // Only resume if we're still on the Sudoku screen and timer was running
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
}
