package com.brainburst.domain.game.zip

import com.brainburst.domain.game.Position

/**
 * Represents the current state of a ZIP game being played
 */
data class ZipState(
    val path: List<Position>,              // Ordered list of positions in the path
    val lastConnectedDotIndex: Int,        // Index of the last correctly connected dot (1 to N)
    val startedAtMillis: Long,             // When the puzzle was started
    val movesCount: Int = 0,               // Number of moves made
    val isCompleted: Boolean = false       // Whether the puzzle is completed
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
     * Check if a position is in the current path
     */
    fun containsPosition(position: Position): Boolean {
        return path.contains(position)
    }
    
    /**
     * Get the last position in the path, or null if empty
     */
    fun lastPosition(): Position? = path.lastOrNull()
}
