package com.brainburst.domain.game.sudoku

import com.brainburst.domain.game.Position

/**
 * Represents the current state of a Sudoku game being played
 */
data class SudokuState(
    val board: List<List<Int>>,           // Current board state
    val fixedCells: Set<Position>,        // Cells from the initial puzzle (cannot be modified)
    val startedAtMillis: Long,            // When the puzzle was started
    val movesCount: Int = 0               // Number of moves made
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
        // Use string building instead of format() for Kotlin Native compatibility
        return buildString {
            append(if (minutes < 10) "0$minutes" else "$minutes")
            append(":")
            append(if (seconds < 10) "0$seconds" else "$seconds")
        }
    }
    
    /**
     * Check if the board is completely filled (no zeros)
     */
    fun isComplete(): Boolean {
        return board.all { row -> row.all { it != 0 } }
    }
}

