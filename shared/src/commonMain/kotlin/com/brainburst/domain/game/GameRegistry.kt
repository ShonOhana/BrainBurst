package com.brainburst.domain.game

import com.brainburst.domain.model.GameType

/**
 * Registry of all game definitions
 * Makes it trivial to add new games - just add a new definition to the list
 * 
 * Usage:
 * ```
 * val definition = gameRegistry.get<Sudoku6x6Payload, SudokuState>(GameType.MINI_SUDOKU_6X6)
 * val payload = definition.decodePayload(jsonElement)
 * val state = definition.initialState(payload)
 * ```
 */
class GameRegistry(
    private val games: List<GameDefinition<*, *>>
) {
    private val byType: Map<GameType, GameDefinition<*, *>> = games.associateBy { it.type }
    
    /**
     * Get a game definition by type
     * @throws IllegalArgumentException if game type is not registered
     */
    @Suppress("UNCHECKED_CAST")
    fun <Payload : Any, State : Any> get(type: GameType): GameDefinition<Payload, State> {
        val definition = byType[type]
            ?: throw IllegalArgumentException("No game definition registered for type: $type")
        
        return definition as GameDefinition<Payload, State>
    }
    
    /**
     * Get all registered game types
     */
    fun getAllTypes(): List<GameType> {
        return byType.keys.toList()
    }
    
    /**
     * Check if a game type is registered
     */
    fun isRegistered(type: GameType): Boolean {
        return byType.containsKey(type)
    }
}



