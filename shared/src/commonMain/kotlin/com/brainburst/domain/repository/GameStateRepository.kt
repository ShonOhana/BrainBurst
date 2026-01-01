package com.brainburst.domain.repository

import com.brainburst.domain.model.SavedGameState

/**
 * Repository for persisting and restoring game state
 */
interface GameStateRepository {
    /**
     * Save the current game state
     */
    suspend fun saveGameState(state: SavedGameState)
    
    /**
     * Load saved game state for a specific puzzle
     */
    suspend fun loadGameState(puzzleId: String): SavedGameState?
    
    /**
     * Clear saved game state for a puzzle (e.g., after completion)
     */
    suspend fun clearGameState(puzzleId: String)
    
    /**
     * Clear all saved game states
     */
    suspend fun clearAllGameStates()
}



