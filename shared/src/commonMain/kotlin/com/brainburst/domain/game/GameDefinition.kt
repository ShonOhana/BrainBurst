package com.brainburst.domain.game

import com.brainburst.domain.model.GameType
import kotlinx.serialization.json.JsonElement

/**
 * Generic interface for game definitions
 * Each game type (Sudoku, Zip, Tango, etc.) implements this interface
 * 
 * @param Payload The game-specific payload type (e.g., Sudoku6x6Payload)
 * @param PlayerState The game-specific state type (e.g., SudokuState)
 */
interface GameDefinition<Payload : Any, PlayerState : Any> {
    /**
     * The type of game this definition handles
     */
    val type: GameType
    
    /**
     * Display name for UI
     */
    val displayName: String
    
    /**
     * Short description of the game
     */
    val description: String
    
    /**
     * Decode the generic JSON payload from Firestore into a typed payload
     */
    fun decodePayload(json: JsonElement): Payload
    
    /**
     * Create the initial player state from the payload
     */
    fun initialState(payload: Payload): PlayerState
    
    /**
     * Apply a game move to the current state and return the new state
     */
    fun applyMove(state: PlayerState, move: GameMove): PlayerState
    
    /**
     * Validate the current state against game rules
     * Returns which positions (if any) violate the rules
     */
    fun validateState(state: PlayerState, payload: Payload): ValidationResult
    
    /**
     * Check if the puzzle is completed correctly
     */
    fun isCompleted(state: PlayerState, payload: Payload): Boolean
}




