package com.brainburst.domain.game.sudoku

import com.brainburst.domain.game.Position

/**
 * Utility functions for validating Sudoku boards
 */
object SudokuValidator {
    
    /**
     * Check if a specific row is valid (no duplicate non-zero values)
     * @return List of positions in the row that have duplicates
     */
    fun getInvalidPositionsInRow(board: List<List<Int>>, row: Int): List<Position> {
        val rowValues = board[row]
        val invalidPositions = mutableListOf<Position>()
        
        for (col in rowValues.indices) {
            val value = rowValues[col]
            if (value != 0) {
                // Check if this value appears elsewhere in the row
                for (otherCol in rowValues.indices) {
                    if (otherCol != col && rowValues[otherCol] == value) {
                        invalidPositions.add(Position(row, col))
                        break
                    }
                }
            }
        }
        
        return invalidPositions
    }
    
    /**
     * Check if a specific column is valid (no duplicate non-zero values)
     * @return List of positions in the column that have duplicates
     */
    fun getInvalidPositionsInColumn(board: List<List<Int>>, col: Int): List<Position> {
        val invalidPositions = mutableListOf<Position>()
        
        for (row in board.indices) {
            val value = board[row][col]
            if (value != 0) {
                // Check if this value appears elsewhere in the column
                for (otherRow in board.indices) {
                    if (otherRow != row && board[otherRow][col] == value) {
                        invalidPositions.add(Position(row, col))
                        break
                    }
                }
            }
        }
        
        return invalidPositions
    }
    
    /**
     * Check if a specific block is valid (no duplicate non-zero values)
     * @param blockStartRow The starting row of the block
     * @param blockStartCol The starting column of the block
     * @param blockRows Number of rows in a block (e.g., 2 for 6x6)
     * @param blockCols Number of columns in a block (e.g., 3 for 6x6)
     * @return List of positions in the block that have duplicates
     */
    fun getInvalidPositionsInBlock(
        board: List<List<Int>>,
        blockStartRow: Int,
        blockStartCol: Int,
        blockRows: Int,
        blockCols: Int
    ): List<Position> {
        val invalidPositions = mutableListOf<Position>()
        val blockValues = mutableListOf<Pair<Position, Int>>()
        
        // Collect all values in the block
        for (r in blockStartRow until blockStartRow + blockRows) {
            for (c in blockStartCol until blockStartCol + blockCols) {
                val value = board[r][c]
                if (value != 0) {
                    blockValues.add(Position(r, c) to value)
                }
            }
        }
        
        // Check for duplicates
        for (i in blockValues.indices) {
            val (pos1, value1) = blockValues[i]
            for (j in i + 1 until blockValues.size) {
                val (pos2, value2) = blockValues[j]
                if (value1 == value2) {
                    invalidPositions.add(pos1)
                    invalidPositions.add(pos2)
                }
            }
        }
        
        return invalidPositions.distinct()
    }
    
    /**
     * Get all invalid positions on the entire board
     * Checks all rows, columns, and blocks
     */
    fun getAllInvalidPositions(
        board: List<List<Int>>,
        blockRows: Int,
        blockCols: Int
    ): List<Position> {
        val invalidPositions = mutableSetOf<Position>()
        
        // Check all rows
        for (row in board.indices) {
            invalidPositions.addAll(getInvalidPositionsInRow(board, row))
        }
        
        // Check all columns
        for (col in board[0].indices) {
            invalidPositions.addAll(getInvalidPositionsInColumn(board, col))
        }
        
        // Check all blocks
        val size = board.size
        for (blockRow in 0 until size step blockRows) {
            for (blockCol in 0 until size step blockCols) {
                invalidPositions.addAll(
                    getInvalidPositionsInBlock(board, blockRow, blockCol, blockRows, blockCols)
                )
            }
        }
        
        return invalidPositions.toList()
    }
    
    /**
     * Check if the entire board is valid (no rule violations)
     */
    fun isBoardValid(
        board: List<List<Int>>,
        blockRows: Int,
        blockCols: Int
    ): Boolean {
        return getAllInvalidPositions(board, blockRows, blockCols).isEmpty()
    }
}




