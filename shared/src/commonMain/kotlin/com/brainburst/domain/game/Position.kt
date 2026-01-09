package com.brainburst.domain.game

import kotlinx.serialization.Serializable

/**
 * Represents a position on a game board (row, column)
 */
@Serializable
data class Position(
    val row: Int,
    val col: Int
) {
    override fun toString(): String = "($row, $col)"
}





