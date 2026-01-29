package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class ThiefStatResponse(
    @SerializedName("memberId")
    val memberId: Long,

    @SerializedName("thiefStat")
    val thiefStat: ThiefStat
)

data class ThiefStat(
    @SerializedName("averageSurvivalTimeSec")
    val averageSurvivalTimeSec: Double,  // 소수가 나올 수 있기 때문에 Int -> Double으로 바꿈

    @SerializedName("escapeCount")
    val escapeCount: Int,

    @SerializedName("grade")
    val grade: String,

    @SerializedName("longestSurvivalSec")
    val longestSurvivalSec: Int,

    @SerializedName("missionClearCount")
    val missionClearCount: Int
)