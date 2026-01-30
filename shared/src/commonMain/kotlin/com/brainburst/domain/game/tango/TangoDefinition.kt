package com.brainburst.domain.game.tango

import com.brainburst.domain.game.GameDefinition
import com.brainburst.domain.game.GameMove
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.TangoMove
import com.brainburst.domain.game.ValidationResult
import com.brainburst.domain.model.GameType
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Game definition for Tango puzzle
 * Fill grid with sun/moon symbols following rules
 */
class TangoDefinition(
    private val json: Json
) : GameDefinition<TangoPayload, TangoState> {
    
    override val type: GameType = GameType.TANGO
    
    override val displayName: String = "Tango"
    
    override val description: String = 
        "Fill the grid with sun and moon symbols. Each row/column has 3 of each. No 3 consecutive identical symbols."
    
    override fun decodePayload(json: JsonElement): TangoPayload {
        return this.json.decodeFromJsonElement(TangoPayload.serializer(), json)
    }
    
    override fun initialState(payload: TangoPayload): TangoState {
        // Build initial cells map from prefilled cells
        val cells = mutableMapOf<Position, CellValue>()
        val fixedCells = mutableSetOf<Position>()
        
        // Initialize all cells as empty
        for (row in 0 until payload.size) {
            for (col in 0 until payload.size) {
                cells[Position(row, col)] = CellValue.EMPTY
            }
        }
        
        // Set prefilled cells
        payload.prefilled.forEach { prefilled ->
            val pos = Position(prefilled.row, prefilled.col)
            cells[pos] = prefilled.value
            fixedCells.add(pos)
        }
        
        return TangoState(
            cells = cells,
            fixedCells = fixedCells,
            startedAtMillis = Clock.System.now().toEpochMilliseconds(),
            movesCount = 0,
            isCompleted = false
        )
    }
    
    override fun applyMove(state: TangoState, move: GameMove): TangoState {
        require(move is TangoMove) { "Move must be a TangoMove" }
        
        // Cannot modify if already completed
        if (state.isCompleted) {
            return state
        }
        
        // Cannot modify fixed cells
        if (state.fixedCells.contains(move.position)) {
            return state
        }
        
        // Update cell value
        val newCells = state.cells.toMutableMap()
        newCells[move.position] = move.value
        
        return state.copy(
            cells = newCells,
            movesCount = state.movesCount + 1
        )
    }
    
    override fun validateState(state: TangoState, payload: TangoPayload): ValidationResult {
        val invalidPositions = mutableListOf<Position>()
        
        // Check each row
        for (row in 0 until payload.size) {
            val rowCells = (0 until payload.size).map { col ->
                Position(row, col) to state.getValue(Position(row, col))
            }
            
            // Count suns and moons
            val sunCount = rowCells.count { it.second == CellValue.SUN }
            val moonCount = rowCells.count { it.second == CellValue.MOON }
            
            // Check if too many of one type
            if (sunCount > 3 || moonCount > 3) {
                rowCells.forEach { (pos, _) ->
                    if (!invalidPositions.contains(pos)) {
                        invalidPositions.add(pos)
                    }
                }
            }
            
            // Check for 3+ consecutive identical symbols
            rowCells.forEachIndexed { index, (pos, value) ->
                if (value != CellValue.EMPTY && index <= rowCells.size - 3) {
                    val next1 = rowCells[index + 1].second
                    val next2 = rowCells[index + 2].second
                    
                    if (value == next1 && value == next2) {
                        if (!invalidPositions.contains(pos)) invalidPositions.add(pos)
                        if (!invalidPositions.contains(rowCells[index + 1].first)) {
                            invalidPositions.add(rowCells[index + 1].first)
                        }
                        if (!invalidPositions.contains(rowCells[index + 2].first)) {
                            invalidPositions.add(rowCells[index + 2].first)
                        }
                    }
                }
            }
        }
        
        // Check each column
        for (col in 0 until payload.size) {
            val colCells = (0 until payload.size).map { row ->
                Position(row, col) to state.getValue(Position(row, col))
            }
            
            // Count suns and moons
            val sunCount = colCells.count { it.second == CellValue.SUN }
            val moonCount = colCells.count { it.second == CellValue.MOON }
            
            // Check if too many of one type
            if (sunCount > 3 || moonCount > 3) {
                colCells.forEach { (pos, _) ->
                    if (!invalidPositions.contains(pos)) {
                        invalidPositions.add(pos)
                    }
                }
            }
            
            // Check for 3+ consecutive identical symbols
            colCells.forEachIndexed { index, (pos, value) ->
                if (value != CellValue.EMPTY && index <= colCells.size - 3) {
                    val next1 = colCells[index + 1].second
                    val next2 = colCells[index + 2].second
                    
                    if (value == next1 && value == next2) {
                        if (!invalidPositions.contains(pos)) invalidPositions.add(pos)
                        if (!invalidPositions.contains(colCells[index + 1].first)) {
                            invalidPositions.add(colCells[index + 1].first)
                        }
                        if (!invalidPositions.contains(colCells[index + 2].first)) {
                            invalidPositions.add(colCells[index + 2].first)
                        }
                    }
                }
            }
        }
        
        // Check equal clues
        payload.equalClues.forEach { clue ->
            val pos1 = Position(clue.row, clue.col)
            val pos2 = when (clue.direction) {
                ClueDirection.HORIZONTAL -> Position(clue.row, clue.col + 1)
                ClueDirection.VERTICAL -> Position(clue.row + 1, clue.col)
            }
            
            val val1 = state.getValue(pos1)
            val val2 = state.getValue(pos2)
            
            // If both filled and not equal, mark as invalid
            if (val1 != CellValue.EMPTY && val2 != CellValue.EMPTY && val1 != val2) {
                if (!invalidPositions.contains(pos1)) invalidPositions.add(pos1)
                if (!invalidPositions.contains(pos2)) invalidPositions.add(pos2)
            }
        }
        
        // Check opposite clues
        payload.oppositeClues.forEach { clue ->
            val pos1 = Position(clue.row, clue.col)
            val pos2 = when (clue.direction) {
                ClueDirection.HORIZONTAL -> Position(clue.row, clue.col + 1)
                ClueDirection.VERTICAL -> Position(clue.row + 1, clue.col)
            }
            
            val val1 = state.getValue(pos1)
            val val2 = state.getValue(pos2)
            
            // If both filled and equal, mark as invalid
            if (val1 != CellValue.EMPTY && val2 != CellValue.EMPTY && val1 == val2) {
                if (!invalidPositions.contains(pos1)) invalidPositions.add(pos1)
                if (!invalidPositions.contains(pos2)) invalidPositions.add(pos2)
            }
        }
        
        return ValidationResult(
            isValid = invalidPositions.isEmpty(),
            invalidPositions = invalidPositions
        )
    }
    
    override fun isCompleted(state: TangoState, payload: TangoPayload): Boolean {
        // All cells must be filled
        if (!state.isComplete()) {
            return false
        }
        
        // Must pass all validations
        val validationResult = validateState(state, payload)
        if (!validationResult.isValid) {
            return false
        }
        
        // Verify each row has exactly 3 suns and 3 moons
        for (row in 0 until payload.size) {
            val rowCells = (0 until payload.size).map { col ->
                state.getValue(Position(row, col))
            }
            val sunCount = rowCells.count { it == CellValue.SUN }
            val moonCount = rowCells.count { it == CellValue.MOON }
            
            if (sunCount != 3 || moonCount != 3) {
                return false
            }
        }
        
        // Verify each column has exactly 3 suns and 3 moons
        for (col in 0 until payload.size) {
            val colCells = (0 until payload.size).map { row ->
                state.getValue(Position(row, col))
            }
            val sunCount = colCells.count { it == CellValue.SUN }
            val moonCount = colCells.count { it == CellValue.MOON }
            
            if (sunCount != 3 || moonCount != 3) {
                return false
            }
        }
        
        return true
    }
}
