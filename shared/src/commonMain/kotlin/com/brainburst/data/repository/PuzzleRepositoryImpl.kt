package com.brainburst.data.repository

import com.brainburst.domain.model.GameType
import com.brainburst.domain.model.PuzzleDto
import com.brainburst.domain.model.ResultDto
import com.brainburst.domain.repository.PuzzleRepository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class PuzzleRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val json: Json = Json { ignoreUnknownKeys = true }
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
            if (!document.exists) {
                return Result.success(null)
            }
            
            // Try to read the document - handle both old format (payload) and new format (payloadJson)
            try {
                // First try the new format with payloadJson string
                val gameTypeStr = document.get<String>("gameType")
                val date = document.get<String>("date")
                val puzzleIdStr = document.get<String>("puzzleId")
                val payloadJsonStr = document.get<String?>("payloadJson")
                
                val payload = if (payloadJsonStr != null) {
                    // New format: parse JSON string
                    json.parseToJsonElement(payloadJsonStr)
                } else {
                    // Old format: try to read payload directly
                    val puzzle = document.data<PuzzleDto>()
                    return Result.success(puzzle)
                }
                
                val puzzle = PuzzleDto(
                    gameType = GameType.valueOf(gameTypeStr),
                    date = date,
                    puzzleId = puzzleIdStr,
                    payload = payload
                )
                Result.success(puzzle)
            } catch (e: Exception) {
                // Fallback to standard deserialization
                val puzzle = document.data<PuzzleDto>()
                Result.success(puzzle)
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
            // Get the latest available puzzle date (today's if exists, otherwise yesterday's)
            // This handles the case where it's after midnight UTC but before 9 AM UTC (when new puzzle is generated)
            val latestDate = getLatestAvailablePuzzleDate(gameType).getOrNull()
            
            // If no puzzle exists at all, user can't have completed it
            if (latestDate == null) {
                return Result.success(false)
            }
            
            val puzzleId = "${gameType.name}_$latestDate"
            
            val results = resultsCollection
                .where { "userId" equalTo userId }
                .where { "puzzleId" equalTo puzzleId }
                .get()
            
            Result.success(results.documents.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getLatestAvailablePuzzleDate(gameType: GameType): Result<String?> {
        return try {
            // First check today's puzzle
            val today = Clock.System.todayIn(TimeZone.UTC)
            val todayStr = today.toString()
            val todayPuzzleId = "${gameType.name}_$todayStr"
            val todayPuzzle = puzzlesCollection.document(todayPuzzleId).get()
            
            if (todayPuzzle.exists) {
                return Result.success(todayStr)
            }
            
            // If today's puzzle doesn't exist, check yesterday's
            val yesterday = today.minus(DatePeriod(days = 1))
            val yesterdayStr = yesterday.toString()
            val yesterdayPuzzleId = "${gameType.name}_$yesterdayStr"
            val yesterdayPuzzle = puzzlesCollection.document(yesterdayPuzzleId).get()
            
            if (yesterdayPuzzle.exists) {
                return Result.success(yesterdayStr)
            }
            
            // No puzzle found for today or yesterday
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

