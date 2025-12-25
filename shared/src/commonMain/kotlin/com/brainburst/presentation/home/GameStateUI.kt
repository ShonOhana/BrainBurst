package com.brainburst.presentation.home

import com.brainburst.domain.model.GameType

/**
 * UI state for a game card on the home screen
 */
sealed class GameStateUI {
    abstract val gameType: GameType
    abstract val title: String
    abstract val subtitle: String
    
    /**
     * Game is available to play
     */
    data class Available(
        override val gameType: GameType,
        override val title: String,
        override val subtitle: String
    ) : GameStateUI()
    
    /**
     * User has completed this game today
     */
    data class Completed(
        override val gameType: GameType,
        override val title: String,
        override val subtitle: String,
        val completionTimeFormatted: String  // e.g., "02:45"
    ) : GameStateUI()
    
    /**
     * Game is coming soon (not yet implemented)
     */
    data class ComingSoon(
        override val gameType: GameType,
        override val title: String,
        override val subtitle: String = "Coming Soon"
    ) : GameStateUI()
    
    /**
     * Game is loading
     */
    data class Loading(
        override val gameType: GameType,
        override val title: String,
        override val subtitle: String = "Loading..."
    ) : GameStateUI()
}

