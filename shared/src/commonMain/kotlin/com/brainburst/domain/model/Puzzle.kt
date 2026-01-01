package com.brainburst.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PuzzleDto(
    val gameType: GameType,
    val date: String,         // yyyy-MM-dd
    val puzzleId: String,
    val payload: JsonElement  // generic payload
)





