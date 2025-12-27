package com.brainburst.domain.game.sudoku

import com.brainburst.domain.game.GameDefinition
import com.brainburst.domain.game.GameMove
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.SudokuMove
import com.brainburst.domain.game.ValidationResult
import com.brainburst.domain.model.GameType
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Game definition for 6x6 Mini Sudoku
 * Implements the generic GameDefinition interface for type-safe game logic
 */
class Sudoku6x6Definition(
    private val json: Json
) : GameDefinition<Sudoku6x6Payload, SudokuState> {
    
    override val type: GameType = GameType.MINI_SUDOKU_6X6
    
    override val displayName: String = "Mini Sudoku 6×6"
    
    override val description: String = 
        "Fill the 6×6 grid with digits 1 to 6 with no repeats in rows, columns, or 2×3 blocks."
    
    override fun decodePayload(json: JsonElement): Sudoku6x6Payload {
        return this.json.decodeFromJsonElement(Sudoku6x6Payload.serializer(), json)
    }
    
    override fun initialState(payload: Sudoku6x6Payload): SudokuState {
        // Find all fixed cells (non-zero values in initial board)
        val fixedCells = mutableSetOf<Position>()
        payload.initialBoard.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, value ->
                if (value != 0) {
                    fixedCells.add(Position(rowIndex, colIndex))
                }
            }
        }
        
        return SudokuState(
            board = payload.initialBoard.map { it.toList() }, // Deep copy
            fixedCells = fixedCells,
            startedAtMillis = Clock.System.now().toEpochMilliseconds(),
            movesCount = 0
        )
    }
    
    override fun applyMove(state: SudokuState, move: GameMove): SudokuState {
        require(move is SudokuMove) { "Move must be a SudokuMove" }
        
        // Cannot modify fixed cells
        if (move.position in state.fixedCells) {
            return state
        }
        
        // Create new board with the move applied
        val newBoard = state.board.mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, value ->
                if (rowIndex == move.position.row && colIndex == move.position.col) {
                    move.value
                } else {
                    value
                }
            }
        }
        
        return state.copy(
            board = newBoard,
            movesCount = state.movesCount + 1
        )
    }
    
    override fun validateState(state: SudokuState, payload: Sudoku6x6Payload): ValidationResult {
        val invalidPositions = SudokuValidator.getAllInvalidPositions(
            board = state.board,
            blockRows = payload.blockRows,
            blockCols = payload.blockCols
        )
        
        return ValidationResult(
            isValid = invalidPositions.isEmpty(),
            invalidPositions = invalidPositions
        )
    }
    
    override fun isCompleted(state: SudokuState, payload: Sudoku6x6Payload): Boolean {
        // Must be completely filled
        if (!state.isComplete()) {
            return false
        }
        
        // Must match the solution exactly
        return state.board == payload.solutionBoard
    }
}


