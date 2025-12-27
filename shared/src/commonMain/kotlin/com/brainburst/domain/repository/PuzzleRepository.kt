package com.brainburst.domain.repository

import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.PuzzleDto
import com.brainburst.domain.model.ResultDto

interface PuzzleRepository {
    /**
     * Get today's puzzle for a specific game type
     */
    suspend fun getTodayPuzzle(gameType: GameType): Result<PuzzleDto?>
    
    /**
     * Get puzzle by ID
     */
    suspend fun getPuzzle(puzzleId: String): Result<PuzzleDto?>
    
    /**
     * Submit a result for a puzzle
     */
    suspend fun submitResult(result: ResultDto): Result<Unit>
    
    /**
     * Get results for a specific puzzle, sorted by duration
     */
    suspend fun getResultsForPuzzle(puzzleId: String, limit: Int = 50): Result<List<ResultDto>>
    
    /**
     * Check if user has already submitted a result for the latest available puzzle.
     * This checks today's puzzle if it exists, otherwise checks yesterday's puzzle.
     * This handles the case where it's after midnight UTC but before the new puzzle is generated (9 AM UTC).
     */
    suspend fun hasUserCompletedToday(userId: String, gameType: GameType): Result<Boolean>
    
    /**
     * Get the latest available puzzle date for a game type (today's if exists, otherwise yesterday's)
     * Returns the date string in yyyy-MM-dd format
     */
    suspend fun getLatestAvailablePuzzleDate(gameType: GameType): Result<String?>
}


