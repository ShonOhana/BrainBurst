package com.brainburst.data.repository

import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.PuzzleDto
import com.brainburst.domain.model.ResultDto
import com.brainburst.domain.repository.PuzzleRepository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class PuzzleRepositoryImpl(
    private val firestore: FirebaseFirestore
) : PuzzleRepository {
    
    private val puzzlesCollection = firestore.collection("puzzles")
    private val resultsCollection = firestore.collection("results")
    
    override suspend fun getTodayPuzzle(gameType: GameType): Result<PuzzleDto?> {
        return try {
            val today = Clock.System.todayIn(TimeZone.UTC).toString()
            val puzzleId = "${gameType.name}_$today"
            getPuzzle(puzzleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getPuzzle(puzzleId: String): Result<PuzzleDto?> {
        return try {
            val document = puzzlesCollection.document(puzzleId).get()
            if (document.exists) {
                val puzzle = document.data<PuzzleDto>()
                Result.success(puzzle)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun submitResult(result: ResultDto): Result<Unit> {
        return try {
            // Check if user already has a result for this puzzle
            val existing = resultsCollection
                .where { "userId" equalTo result.userId }
                .where { "puzzleId" equalTo result.puzzleId }
                .get()
            
            if (existing.documents.isNotEmpty()) {
                return Result.failure(Exception("User has already submitted a result for this puzzle"))
            }
            
            // Add new result
            resultsCollection.add(result)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getResultsForPuzzle(puzzleId: String, limit: Int): Result<List<ResultDto>> {
        return try {
            val results = resultsCollection
                .where { "puzzleId" equalTo puzzleId }
                .orderBy("durationMs", Direction.ASCENDING)
                .limit(limit)
                .get()
                .documents
                .map { it.data<ResultDto>() }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun hasUserCompletedToday(userId: String, gameType: GameType): Result<Boolean> {
        return try {
            val today = Clock.System.todayIn(TimeZone.UTC).toString()
            val puzzleId = "${gameType.name}_$today"
            
            val results = resultsCollection
                .where { "userId" equalTo userId }
                .where { "puzzleId" equalTo puzzleId }
                .get()
            
            Result.success(results.documents.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

