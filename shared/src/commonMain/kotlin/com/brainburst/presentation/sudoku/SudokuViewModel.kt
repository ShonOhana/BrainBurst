package com.brainburst.presentation.sudoku

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
    private val viewModelScope: CoroutineScope
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
    private var lastSaveTimeMillis: Long = 0L
    
    init {
        loadPuzzle()
    }
    
    private fun loadPuzzle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Get today's puzzle
            val result = puzzleRepository.getTodayPuzzle(GameType.MINI_SUDOKU_6X6)
            
            result.fold(
                onSuccess = { puzzleDto ->
                    if (puzzleDto == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "No puzzle available for today. Please try again later."
                        )
                        return@launch
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
        val now = Clock.System.now().toEpochMilliseconds()
        val sessionElapsed = now - timerStartedAtMillis
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
        
        // Auto-save every 5 seconds to handle app being killed
        if (now - lastSaveTimeMillis >= 5000) {
            saveGameState()
            lastSaveTimeMillis = now
        }
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
        
        // Update timer display
        updateTimer()
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
        
        // Save state after move
        saveGameState()
    }
    
    fun onErasePress() {
        val position = _uiState.value.selectedPosition ?: return
        onNumberPress(0) // 0 means erase
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
            // Stop timer
            pauseTimer()
            
            // Calculate total duration
            val durationMs = elapsedMillisWhenPaused
            
            // Emit completion event
            _events.value = SudokuEvent.PuzzleCompleted(
                durationMs = durationMs,
                movesCount = state.movesCount
            )
            
            // Submit result to Firestore
            submitResult(durationMs, state.movesCount)
            
            // Clear saved state
            viewModelScope.launch {
                puzzleId?.let { gameStateRepository.clearGameState(it) }
            }
            
            // Navigate to leaderboard
            navigator.navigateTo(Screen.Leaderboard(GameType.MINI_SUDOKU_6X6))
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
    
    private fun submitResult(durationMs: Long, movesCount: Int) {
        viewModelScope.launch {
            val user = authRepository.currentUser.value ?: return@launch
            val puzzleDto = puzzleRepository.getTodayPuzzle(GameType.MINI_SUDOKU_6X6).getOrNull() ?: return@launch
            
            val result = ResultDto(
                userId = user.uid,
                puzzleId = puzzleDto.puzzleId,
                gameType = GameType.MINI_SUDOKU_6X6,
                date = puzzleDto.date,
                durationMs = durationMs,
                movesCount = movesCount
            )
            
            puzzleRepository.submitResult(result)
        }
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
}
