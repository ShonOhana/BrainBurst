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
    val errorMessage: String? = null,
    val hintPosition: Position? = null,
    val hintType: HintType = HintType.None
)

data class ZipDotUi(
    val position: Position,
    val index: Int
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
    private var solutionCache: List<Position>? = null

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
                        onSuccess = puzzleSuccess@{ puzzleDto ->
                            if (puzzleDto == null) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "No puzzle available. Please check back later."
                                )
                                return@puzzleSuccess
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
    
    /**
     * Solve the ZIP puzzle using backtracking DFS
     * Returns the complete solution path from dot 1 to the last dot
     */
    private fun solvePuzzle(payload: ZipPayload): List<Position>? {
        val gridSize = payload.size
        val totalCells = gridSize * gridSize
        val solution = mutableListOf<Position>()
        val visited = mutableSetOf<Position>()
        val startPos = payload.getDot(1)?.toPosition() ?: return null
        
        fun dfs(current: Position, dotIndex: Int): Boolean {
            solution.add(current)
            visited.add(current)
            
            // Success: all cells visited and all dots connected
            if (solution.size == totalCells && dotIndex == payload.dotCount) {
                return true
            }
            
            // Determine if we need to reach next dot
            val nextDotIndex = dotIndex + 1
            val nextDot = payload.getDotPosition(nextDotIndex)
            
            // Get adjacent cells
            val adjacent = getAdjacentPositions(current, gridSize)
            
            // If we need to reach next dot, prioritize moves toward it
            val sortedAdjacent = if (nextDot != null && nextDotIndex <= payload.dotCount) {
                adjacent.sortedBy { pos ->
                    kotlin.math.abs(pos.row - nextDot.row) + kotlin.math.abs(pos.col - nextDot.col)
                }
            } else {
                adjacent
            }
            
            for (next in sortedAdjacent) {
                if (next in visited) continue
                
                // Check if this is the next dot
                val isNextDot = next == nextDot
                
                // Pruning: if we need a specific dot and this isn't it, 
                // make sure we can still reach it
                if (nextDot != null && !isNextDot && nextDotIndex <= payload.dotCount) {
                    // Check if we're blocking path to next dot
                    if (!canStillReachDot(next, nextDot, visited, totalCells - solution.size - 1)) {
                        continue
                    }
                }
                
                val newDotIndex = if (isNextDot) dotIndex + 1 else dotIndex
                
                if (dfs(next, newDotIndex)) {
                    return true
                }
            }
            
            // Backtrack
            solution.removeLast()
            visited.remove(current)
            return false
        }
        
        return if (dfs(startPos, 1)) solution else null
    }
    
    /**
     * Check if we can still reach a target dot from current position
     */
    private fun canStillReachDot(
        from: Position,
        target: Position,
        visited: Set<Position>,
        @Suppress("UNUSED_PARAMETER") remainingCells: Int
    ): Boolean {
        // Simple reachability check using BFS
        if (from == target) return true
        
        val queue = mutableListOf(from)
        val tempVisited = visited.toMutableSet()
        tempVisited.add(from)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            
            for (next in getAdjacentPositions(current, 6)) {
                if (next in tempVisited) continue
                
                if (next == target) return true
                
                tempVisited.add(next)
                queue.add(next)
            }
        }
        
        return false
    }
    
    /**
     * Get adjacent positions (up, down, left, right)
     */
    private fun getAdjacentPositions(pos: Position, gridSize: Int): List<Position> {
        return listOf(
            Position(pos.row - 1, pos.col),
            Position(pos.row + 1, pos.col),
            Position(pos.row, pos.col - 1),
            Position(pos.row, pos.col + 1)
        ).filter { it.row in 0 until gridSize && it.col in 0 until gridSize }
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
     * Check if the next move is forced (only valid continuation)
     */
    private fun isForced(
        state: ZipState,
        nextPos: Position,
        @Suppress("UNUSED_PARAMETER") solution: List<Position>,
        payload: ZipPayload
    ): Boolean {
        val current = state.lastPosition() ?: return false
        val adjacent = getAdjacentPositions(current, payload.size)
        val unvisited = adjacent.filter { !state.containsPosition(it) }
        
        // Only one option available
        if (unvisited.size <= 1) return true
        
        // Multiple options: check if only one doesn't create dead end
        // For simplicity, we trust the solution path
        return unvisited.size == 1 && unvisited.first() == nextPos
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
        payload: ZipPayload
    ): HintAction {
        // Edge case: puzzle complete
        if (state.isCompleted) return HintAction.NoHint
        
        // Edge case: empty path (should not happen, but handle it)
        if (state.path.isEmpty()) {
            return HintAction.HighlightDot(1)
        }
        
        // Find divergence point
        val divergenceIndex = findDivergence(state.path, solution)
        
        // User is on correct path
        if (divergenceIndex == state.path.size) {
            // Check if next move is forced (only 1 valid continuation)
            val nextPos = solution.getOrNull(state.path.size)
            if (nextPos != null && isForced(state, nextPos, solution, payload)) {
                return HintAction.HighlightCell(nextPos)
            }
            
            // Multiple valid continuations - show next dot
            val nextDotIndex = state.lastConnectedDotIndex + 1
            if (nextDotIndex <= payload.dotCount) {
                return HintAction.HighlightDot(nextDotIndex)
            }
            
            return HintAction.NoHint  // User can explore
        }
        
        // User diverged - suggest undo from divergence point
        return HintAction.SuggestUndo(state.path[divergenceIndex])
    }

    fun onHintPress() {
        val state = currentState ?: return
        val payloadData = payload ?: return
        
        println("ZIP HINT: Starting hint calculation")
        println("ZIP HINT: Current path size: ${state.path.size}")
        
        // Pause timer during hint
        pauseTimer()
        
        viewModelScope.launch {
            // Show rewarded ad first
            adManager.showRewardedAd {
                viewModelScope.launch adCallback@{
                    // Solve puzzle (use cached solution if available)
                    val solution = solutionCache ?: solvePuzzle(payloadData)?.also { 
                        solutionCache = it 
                    }
                    
                    if (solution == null) {
                        // No solution exists (should not happen with valid puzzles)
                        println("ZIP HINT: No solution found!")
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
                }
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
