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

