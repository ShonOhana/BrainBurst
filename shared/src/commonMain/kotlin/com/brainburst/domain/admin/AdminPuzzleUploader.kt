package com.brainburst.domain.admin

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Admin utility to upload test puzzles to Firestore
 * Use this to quickly add puzzles during development/testing
 */
class AdminPuzzleUploader(
    private val firestore: FirebaseFirestore,
    private val json: Json
) {
    
    /**
     * Upload today's test puzzle to Firestore
     * Call this once from the app to add a test puzzle
     * 
     * Note: We store payload as a JSON string because GitLive Firebase
     * doesn't support nested arrays. The app will parse it when reading.
     */
    suspend fun uploadTodayTestPuzzle(): Result<String> {
        return try {
            val today = Clock.System.todayIn(TimeZone.UTC).toString()
            val puzzleId = "MINI_SUDOKU_6X6_$today"
            
            // Create payload as JSON object
            val payloadJson = buildJsonObject {
                put("size", JsonPrimitive(6))
                put("blockRows", JsonPrimitive(2))
                put("blockCols", JsonPrimitive(3))
                
                // Build nested arrays as JSON
                put("initialBoard", buildJsonArray {
                    add(buildJsonArray { listOf(1, 0, 3, 0, 5, 0).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(0, 5, 0, 1, 0, 3).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(2, 0, 4, 0, 6, 0).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(0, 6, 0, 2, 0, 4).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(3, 0, 5, 0, 1, 0).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(0, 1, 0, 3, 0, 5).forEach { add(JsonPrimitive(it)) } })
                })
                
                put("solutionBoard", buildJsonArray {
                    add(buildJsonArray { listOf(1, 2, 3, 4, 5, 6).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(4, 5, 6, 1, 2, 3).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(2, 3, 4, 5, 6, 1).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(5, 6, 1, 2, 3, 4).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(3, 4, 5, 6, 1, 2).forEach { add(JsonPrimitive(it)) } })
                    add(buildJsonArray { listOf(6, 1, 2, 3, 4, 5).forEach { add(JsonPrimitive(it)) } })
                })
            }
            
            // Convert to JSON string for storage (workaround for GitLive limitations)
            val payloadString = json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                payloadJson
            )
            
            // Create the puzzle data
            val puzzleData = mapOf(
                "gameType" to "MINI_SUDOKU_6X6",
                "date" to today,
                "puzzleId" to puzzleId,
                "payloadJson" to payloadString  // Store as string, will parse when reading
            )
            
            // Upload to Firestore
            firestore.collection("puzzles")
                .document(puzzleId)
                .set(puzzleData)
            
            Result.success("✅ Puzzle uploaded! ID: $puzzleId")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
}

