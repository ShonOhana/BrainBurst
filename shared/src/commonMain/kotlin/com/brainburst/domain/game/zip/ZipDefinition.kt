package com.brainburst.domain.game.zip

import com.brainburst.domain.game.GameDefinition
import com.brainburst.domain.game.GameMove
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.ValidationResult
import com.brainburst.domain.game.ZipMove
import com.brainburst.domain.model.GameType
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.math.abs

/**
 * Game definition for ZIP puzzle
 * Connect numbered dots in order with a continuous path
 */
class ZipDefinition(
    private val json: Json
) : GameDefinition<ZipPayload, ZipState> {
    
    override val type: GameType = GameType.ZIP
    
    override val displayName: String = "Zip"
    
    override val description: String = 
        "Connect numbered dots in order with a continuous path. No crossing, no revisiting cells."
    
    override fun decodePayload(json: JsonElement): ZipPayload {
        return this.json.decodeFromJsonElement(ZipPayload.serializer(), json)
    }
    
    override fun initialState(payload: ZipPayload): ZipState {
        // Start at dot 1
        val dot1 = payload.getDot(1) 
            ?: throw IllegalStateException("ZIP puzzle must have dot 1")
        
        return ZipState(
            path = listOf(dot1.toPosition()),
            lastConnectedDotIndex = 1,
            startedAtMillis = Clock.System.now().toEpochMilliseconds(),
            movesCount = 0,
            isCompleted = false
        )
    }
    
    override fun applyMove(state: ZipState, move: GameMove): ZipState {
        require(move is ZipMove) { "Move must be a ZipMove" }
        
        // Cannot move if already completed
        if (state.isCompleted) {
            return state
        }
        
        val lastPos = state.lastPosition() ?: return state
        
        // Check if move is orthogonally adjacent
        if (!isAdjacent(lastPos, move.position)) {
            return state // Invalid move, not adjacent
        }
        
        // Check if position was already visited
        if (state.containsPosition(move.position)) {
            return state // Invalid move, revisiting cell
        }
        
        // Valid move - add to path
        val newPath = state.path + move.position
        
        return state.copy(
            path = newPath,
            movesCount = state.movesCount + 1
        )
    }
    
    /**
     * Check if a move is blocked by a wall
     */
    private fun isBlockedByWall(from: Position, to: Position, walls: List<ZipWall>): Boolean {
        // Determine direction of movement
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        
        // Check walls from the 'from' cell
        for (wall in walls) {
            if (wall.row == from.row && wall.col == from.col) {
                when (wall.side) {
                    WallSide.TOP -> if (rowDiff == -1 && colDiff == 0) return true
                    WallSide.RIGHT -> if (rowDiff == 0 && colDiff == 1) return true
                    WallSide.BOTTOM -> if (rowDiff == 1 && colDiff == 0) return true
                    WallSide.LEFT -> if (rowDiff == 0 && colDiff == -1) return true
                }
            }
            // Check walls from the 'to' cell (opposite sides)
            if (wall.row == to.row && wall.col == to.col) {
                when (wall.side) {
                    WallSide.TOP -> if (rowDiff == 1 && colDiff == 0) return true
                    WallSide.RIGHT -> if (rowDiff == 0 && colDiff == -1) return true
                    WallSide.BOTTOM -> if (rowDiff == -1 && colDiff == 0) return true
                    WallSide.LEFT -> if (rowDiff == 0 && colDiff == 1) return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Apply move with wall checking (for ViewModel use)
     */
    fun applyMoveWithWalls(state: ZipState, move: GameMove, payload: ZipPayload): ZipState {
        require(move is ZipMove) { "Move must be a ZipMove" }
        
        // Cannot move if already completed
        if (state.isCompleted) {
            return state
        }
        
        val lastPos = state.lastPosition() ?: return state
        
        // Check if move is orthogonally adjacent
        if (!isAdjacent(lastPos, move.position)) {
            return state // Invalid move, not adjacent
        }
        
        // Check if position was already visited
        if (state.containsPosition(move.position)) {
            return state // Invalid move, revisiting cell
        }
        
        // Check if move is blocked by wall
        if (isBlockedByWall(lastPos, move.position, payload.walls)) {
            return state // Invalid move, blocked by wall
        }
        
        // Valid move - add to path
        val newPath = state.path + move.position
        
        return state.copy(
            path = newPath,
            movesCount = state.movesCount + 1
        )
    }
    
    override fun validateState(state: ZipState, payload: ZipPayload): ValidationResult {
        // Check which dot we should be at based on the path
        var expectedDotIndex = state.lastConnectedDotIndex
        
        // Check if we've reached any new dots
        for (i in (state.lastConnectedDotIndex + 1)..payload.dotCount) {
            val dotPos = payload.getDotPosition(i)
            if (dotPos != null && state.containsPosition(dotPos)) {
                // Check if all previous dots were connected
                var allPreviousConnected = true
                for (j in 1 until i) {
                    val prevDotPos = payload.getDotPosition(j)
                    if (prevDotPos == null || !state.containsPosition(prevDotPos)) {
                        allPreviousConnected = false
                        break
                    }
                }
                
                if (allPreviousConnected) {
                    expectedDotIndex = i
                }
            }
        }
        
        // Update the state's lastConnectedDotIndex if needed
        // (This is a side-effect that should be handled in applyMove, but we track it here)
        
        return ValidationResult(
            isValid = true,
            invalidPositions = emptyList()
        )
    }
    
    override fun isCompleted(state: ZipState, payload: ZipPayload): Boolean {
        // Must fill the entire 6x6 board (36 cells)
        if (state.path.size != 36) {
            return false
        }
        
        // The LAST position must be the last dot
        val lastDotPos = payload.getDotPosition(payload.dotCount) ?: return false
        if (state.lastPosition() != lastDotPos) {
            return false
        }
        
        // Verify all dots are connected in order
        for (i in 1..payload.dotCount) {
            val dotPos = payload.getDotPosition(i) ?: return false
            if (!state.containsPosition(dotPos)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Check if two positions are orthogonally adjacent
     */
    private fun isAdjacent(pos1: Position, pos2: Position): Boolean {
        val rowDiff = abs(pos1.row - pos2.row)
        val colDiff = abs(pos1.col - pos2.col)
        
        // Must be exactly 1 step in one direction, 0 in the other
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }
    
    /**
     * Update state to track which dot index we're at
     * This should be called after each move to update lastConnectedDotIndex
     */
    fun updateDotProgress(state: ZipState, payload: ZipPayload): ZipState {
        var lastConnected = state.lastConnectedDotIndex
        
        // Check if we've reached the next dot(s)
        for (i in (lastConnected + 1)..payload.dotCount) {
            val dotPos = payload.getDotPosition(i)
            if (dotPos != null && state.lastPosition() == dotPos) {
                lastConnected = i
            } else {
                break // Must be sequential
            }
        }
        
        // Only complete if: all cells filled, all dots connected, AND last position is the last dot
        val lastDotPos = payload.getDotPosition(payload.dotCount)
        val completed = lastConnected == payload.dotCount && 
                       state.path.size == payload.size * payload.size &&
                       state.lastPosition() == lastDotPos
        
        return state.copy(
            lastConnectedDotIndex = lastConnected,
            isCompleted = completed
        )
    }
}
