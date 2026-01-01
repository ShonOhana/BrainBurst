package com.brainburst.domain.game

/**
 * Result of validating the current game state
 * @param isValid Whether the current state follows all game rules
 * @param invalidPositions Positions that violate game rules (for visual feedback)
 */
data class ValidationResult(
    val isValid: Boolean,
    val invalidPositions: List<Position> = emptyList()
)



