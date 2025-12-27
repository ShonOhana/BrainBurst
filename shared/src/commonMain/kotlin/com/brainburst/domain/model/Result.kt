package com.brainburst.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ResultDto(
    val userId: String,
    val puzzleId: String,
    val gameType: GameType,
    val date: String,
    val durationMs: Long,
    val movesCount: Int
)



