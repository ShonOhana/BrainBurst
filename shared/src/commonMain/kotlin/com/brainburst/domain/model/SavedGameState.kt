package com.brainburst.domain.model

import com.brainburst.domain.game.Position
import kotlinx.serialization.Serializable

/**
 * Serializable representation of a saved game state for persistence
 */
@Serializable
data class SavedGameState(
    val puzzleId: String,
    val gameType: GameType,
    val board: List<List<Int>>,
    val fixedCells: List<SerializablePosition>,
    val startedAtMillis: Long,
    val movesCount: Int,
    val elapsedMillisAtPause: Long,  // Total elapsed time when paused
    val lastSavedAtMillis: Long       // When this state was saved
)

@Serializable
data class SerializablePosition(
    val row: Int,
    val col: Int
) {
    fun toPosition() = Position(row, col)
}

fun Position.toSerializable() = SerializablePosition(row, col)

fun Set<Position>.toSerializable() = map { it.toSerializable() }



