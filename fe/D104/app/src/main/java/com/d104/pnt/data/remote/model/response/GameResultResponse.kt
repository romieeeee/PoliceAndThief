package com.d104.pnt.data.remote.model.response

data class GameResultResponse(
    val gameId: Long,
    val winner: String, // "POLICE" 또는 "THIEF"
    val endedAt: String,
    val stats: GameStats,
    val mvp: PlayerResult?,
    val winningSecond: PlayerResult?,
    val losingFirst: PlayerResult?
)

data class GameStats(
    val arrests: Int,
    val missionsCleared: Int,
    val durationSec: Long
)

data class PlayerResult(
    val memberId: Long,
    val nickname: String,
    val role: String,
    val description: String
)