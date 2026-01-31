package com.brainburst.presentation.tango

import com.brainburst.domain.ads.AdManager
import com.brainburst.domain.game.GameRegistry
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.TangoMove
import com.brainburst.domain.game.tango.CellValue
import com.brainburst.domain.game.tango.ClueDirection
import com.brainburst.domain.game.tango.EqualClue
import com.brainburst.domain.game.tango.OppositeClue
import com.brainburst.domain.game.tango.TangoDefinition
import com.brainburst.domain.game.tango.TangoPayload
import com.brainburst.domain.game.tango.TangoState
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

data class TangoUiState(
    val cells: Map<Position, CellValue> = emptyMap(),
    val fixedCells: Set<Position> = emptySet(),
    val selectedPosition: Position? = null,
    val invalidPositions: List<Position> = emptyList(),
    val equalClues: List<EqualClue> = emptyList(),
    val oppositeClues: List<OppositeClue> = emptyList(),
    val elapsedTimeFormatted: String = "00:00",
    val movesCount: Int = 0,
    val isComplete: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val showCompletionAnimation: Boolean = false
)

sealed class TangoEvent {
    data class PuzzleCompleted(
        val durationMs: Long,
        val movesCount: Int
    ) : TangoEvent()

    data class ValidationError(val message: String) : TangoEvent()
}

class TangoViewModel(
    private val gameRegistry: GameRegistry,
    private val puzzleRepository: PuzzleRepository,
    private val authRepository: AuthRepository,
    private val gameStateRepository: GameStateRepository,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope,
    private val adManager: AdManager
) {
    private val _uiState = MutableStateFlow(TangoUiState())
    val uiState: StateFlow<TangoUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<TangoEvent?>(null)
    val events: StateFlow<TangoEvent?> = _events.asStateFlow()

    private var tangoDefinition: TangoDefinition? = null
    private var payload: TangoPayload? = null
    private var currentState: TangoState? = null
    private var timerJob: Job? = null
    private var puzzleId: String? = null
    private var puzzleDate: String? = null

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

            val latestDateResult = puzzleRepository.getLatestAvailablePuzzleDate(GameType.TANGO)
            
            latestDateResult.fold(
                onSuccess = { latestDate ->
                    if (latestDate == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "No puzzle available. Please check back later."
                        )
                        return@launch
                    }
                    
                    val puzzleIdForDate = "${GameType.TANGO.name}_$latestDate"
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

                            if (puzzleId != null && puzzleId != puzzleDto.puzzleId) {
                                gameStateRepository.clearGameState(puzzleId!!)
                            }

                            puzzleId = puzzleDto.puzzleId
                            puzzleDate = puzzleDto.date

                            // Decode payload
                            val definition = gameRegistry.get<TangoPayload, TangoState>(GameType.TANGO)
                            tangoDefinition = definition as TangoDefinition
                            payload = definition.decodePayload(puzzleDto.payload)

                            // Try to restore saved state
                            val savedState = gameStateRepository.loadGameState(puzzleDto.puzzleId)

                            if (savedState != null) {
                                // Restore from saved state
                                val cellsMap = mutableMapOf<Position, CellValue>()
                                savedState.board.forEachIndexed { row, rowList ->
                                    rowList.forEachIndexed { col, value ->
                                        cellsMap[Position(row, col)] = when (value) {
                                            1 -> CellValue.SUN
                                            2 -> CellValue.MOON
                                            else -> CellValue.EMPTY
                                        }
                                    }
                                }
                                
                                currentState = TangoState(
                                    cells = cellsMap,
                                    fixedCells = savedState.fixedCells.map { it.toPosition() }.toSet(),
                                    startedAtMillis = savedState.startedAtMillis,
                                    movesCount = savedState.movesCount,
                                    isCompleted = false
                                )
                                
                                elapsedMillisWhenPaused = savedState.elapsedMillisAtPause
                            } else {
                                // Create initial state
                                currentState = definition.initialState(payload!!)
                                elapsedMillisWhenPaused = 0L
                            }

                            // Validate and update UI
                            validateCurrentState()
                            updateUiFromState()
                            startTimer()

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                equalClues = payload!!.equalClues,
                                oppositeClues = payload!!.oppositeClues
                            )
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
                        errorMessage = error.message ?: "Failed to load puzzle"
                    )
                }
            )
        }
    }

    private fun startTimer() {
        if (isTimerRunning) return
        
        isTimerRunning = true
        timerStartedAtMillis = Clock.System.now().toEpochMilliseconds() - elapsedMillisWhenPaused
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isTimerRunning) {
                delay(1000)
                updateElapsedTime()
            }
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerJob?.cancel()
        
        val now = Clock.System.now().toEpochMilliseconds()
        elapsedMillisWhenPaused = now - timerStartedAtMillis
    }

    private fun updateElapsedTime() {
        val state = currentState ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - timerStartedAtMillis
        
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / 1000) / 60
        
        _uiState.value = _uiState.value.copy(
            elapsedTimeFormatted = buildString {
                append(if (minutes < 10) "0$minutes" else "$minutes")
                append(":")
                append(if (seconds < 10) "0$seconds" else "$seconds")
            }
        )
    }

    fun onCellClick(position: Position) {
        if (_uiState.value.fixedCells.contains(position)) {
            return  // Cannot modify fixed cells
        }
        
        val definition = tangoDefinition ?: return
        val state = currentState ?: return
        
        // Cycle through values: EMPTY → SUN → MOON → EMPTY
        val currentValue = state.getValue(position)
        val nextValue = when (currentValue) {
            CellValue.EMPTY -> CellValue.SUN
            CellValue.SUN -> CellValue.MOON
            CellValue.MOON -> CellValue.EMPTY
        }
        
        // Apply move
        val move = TangoMove(position, nextValue)
        val newState = definition.applyMove(state, move)
        currentState = newState

        // Validate
        validateCurrentState()
        updateUiFromState()
        
        // Save state
        saveGameState()

        // Check if completed
        if (definition.isCompleted(newState, payload!!)) {
            onPuzzleCompleted()
        }
    }

    private fun validateCurrentState() {
        val definition = tangoDefinition ?: return
        val state = currentState ?: return
        val p = payload ?: return

        val validationResult = definition.validateState(state, p)
        
        _uiState.value = _uiState.value.copy(
            invalidPositions = validationResult.invalidPositions
        )
    }

    private fun updateUiFromState() {
        val state = currentState ?: return
        
        _uiState.value = _uiState.value.copy(
            cells = state.cells,
            fixedCells = state.fixedCells,
            movesCount = state.movesCount,
            isComplete = state.isComplete()
        )
    }

    private fun saveGameState() {
        val state = currentState ?: return
        val id = puzzleId ?: return

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val elapsed = if (isTimerRunning) {
                now - timerStartedAtMillis
            } else {
                elapsedMillisWhenPaused
            }

            // Convert cells map to 2D list
            val board = List(6) { row ->
                List(6) { col ->
                    when (state.getValue(Position(row, col))) {
                        CellValue.SUN -> 1
                        CellValue.MOON -> 2
                        CellValue.EMPTY -> 0
                    }
                }
            }

            val savedState = SavedGameState(
                puzzleId = id,
                gameType = GameType.TANGO,
                board = board,
                fixedCells = state.fixedCells.map { it.toSerializable() },
                startedAtMillis = state.startedAtMillis,
                movesCount = state.movesCount,
                elapsedMillisAtPause = elapsed,
                lastSavedAtMillis = now
            )

            gameStateRepository.saveGameState(savedState)
        }
    }

    private fun onPuzzleCompleted() {
        stopTimer()
        
        _uiState.value = _uiState.value.copy(showCompletionAnimation = true)
        
        viewModelScope.launch {
            delay(1500)
            submitResult()
        }
    }

    private fun submitResult() {
        val state = currentState ?: return
        val id = puzzleId ?: return
        val date = puzzleDate ?: return
        val user = authRepository.currentUser.value ?: return

        _uiState.value = _uiState.value.copy(isSubmitting = true)

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val durationMs = now - timerStartedAtMillis

            val result = ResultDto(
                userId = user.uid,
                displayName = user.displayName ?: "Anonymous",
                puzzleId = id,
                gameType = GameType.TANGO,
                date = date,
                durationMs = durationMs,
                movesCount = state.movesCount
            )

            puzzleRepository.submitResult(result).fold(
                onSuccess = {
                    gameStateRepository.clearGameState(id)
                    
                    _events.value = TangoEvent.PuzzleCompleted(
                        durationMs = durationMs,
                        movesCount = state.movesCount
                    )
                    
                    delay(500)
                    navigator.navigateTo(Screen.Leaderboard(GameType.TANGO))
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Failed to submit result"
                    )
                }
            )
        }
    }

    fun onBackClick() {
        stopTimer()
        saveGameState()
        navigator.navigateTo(Screen.Home)
    }
    
    fun onClearClick() {
        val definition = tangoDefinition ?: return
        val p = payload ?: return
        val id = puzzleId ?: return
        
        // Create fresh initial state
        currentState = definition.initialState(p)
        
        // Reset timer
        stopTimer()
        elapsedMillisWhenPaused = 0L
        
        // Update UI
        validateCurrentState()
        updateUiFromState()
        
        // Clear saved state
        viewModelScope.launch {
            gameStateRepository.clearGameState(id)
        }
        
        // Restart timer
        startTimer()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun consumeEvent() {
        _events.value = null
    }

    fun onPause() {
        stopTimer()
        saveGameState()
    }

    fun onResume() {
        if (!_uiState.value.isComplete && currentState != null) {
            startTimer()
        }
    }
}
