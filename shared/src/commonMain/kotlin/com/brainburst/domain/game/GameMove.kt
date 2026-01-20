package com.brainburst.domain.game

/**
 * Base interface for all game moves across different game types
 */
sealed interface GameMove

/**
 * Represents a move in Sudoku where a number is placed at a position
 */
data class SudokuMove(
    val position: Position,
    val value: Int  // 0 to clear, 1-6 for Mini Sudoku 6x6
) : GameMove

/**
 * Represents a move in ZIP where a cell is added to the path
 */
data class ZipMove(
    val position: Position
) : GameMove





