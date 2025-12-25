package com.brainburst.presentation.sudoku

import com.brainburst.domain.game.GameRegistry
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.SudokuMove
import com.brainburst.domain.game.sudoku.Sudoku6x6Definition
import com.brainburst.domain.game.sudoku.Sudoku6x6Payload
import com.brainburst.domain.game.sudoku.SudokuState
import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.ResultDto
import com.brainburst.domain.repository.AuthRepository
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
                    
                    // Decode payload
                    val definition = gameRegistry.get<Sudoku6x6Payload, SudokuState>(GameType.MINI_SUDOKU_6X6)
                    sudokuDefinition = definition as Sudoku6x6Definition
                    payload = definition.decodePayload(puzzleDto.payload)
                    
                    // Initialize state
                    currentState = definition.initialState(payload!!)
                    
                    // Update UI
                    updateUiFromState()
                    
                    // Start timer
                    startTimer()
                    
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
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                updateTimer()
            }
        }
    }
    
    private fun updateTimer() {
        val state = currentState ?: return
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val formatted = state.getFormattedTime(currentTime)
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
            timerJob?.cancel()
            
            // Calculate duration
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val durationMs = state.getElapsedMillis(currentTime)
            
            // Emit completion event
            _events.value = SudokuEvent.PuzzleCompleted(
                durationMs = durationMs,
                movesCount = state.movesCount
            )
            
            // Submit result to Firestore
            submitResult(durationMs, state.movesCount)
            
            // Navigate to leaderboard (TODO: Add rewarded ad before this in Phase 7)
            navigator.navigateTo(Screen.Leaderboard(GameType.MINI_SUDOKU_6X6))
        } else {
            _events.value = SudokuEvent.ValidationError("Solution is incorrect. Please check your answers.")
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
        timerJob?.cancel()
        navigator.navigateTo(Screen.Home)
    }
    
    fun clearEvent() {
        _events.value = null
    }
    
    fun onCleared() {
        timerJob?.cancel()
    }
}

