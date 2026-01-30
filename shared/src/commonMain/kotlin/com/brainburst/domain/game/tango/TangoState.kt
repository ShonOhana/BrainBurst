package com.brainburst.domain.game.tango

import com.brainburst.domain.game.Position

/**
 * Represents the current state of a Tango game being played
 */
data class TangoState(
    val cells: Map<Position, CellValue>,      // Current board state
    val fixedCells: Set<Position>,            // Cells from the puzzle (cannot be modified)
    val startedAtMillis: Long,                // When the puzzle was started
    val movesCount: Int = 0,                  // Number of moves made
    val isCompleted: Boolean = false          // Whether the puzzle is completed
) {
    /**
     * Get elapsed time in milliseconds
     */
    fun getElapsedMillis(currentTimeMillis: Long): Long {
        return currentTimeMillis - startedAtMillis
    }
    
    /**
     * Format elapsed time as mm:ss
     */
    fun getFormattedTime(currentTimeMillis: Long): String {
        val elapsedSeconds = (currentTimeMillis - startedAtMillis) / 1000
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        return buildString {
            append(if (minutes < 10) "0$minutes" else "$minutes")
            append(":")
            append(if (seconds < 10) "0$seconds" else "$seconds")
        }
    }
    
    /**
     * Get the value at a position
     */
    fun getValue(position: Position): CellValue {
        return cells[position] ?: CellValue.EMPTY
    }
    
    /**
     * Check if all cells are filled
     */
    fun isComplete(): Boolean {
        return cells.values.none { it == CellValue.EMPTY }
    }
}
