package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class MissionSocketDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("gameId")
    val gameId: Long,
    @SerializedName("missionId")
    val missionId: Long,
    @SerializedName("completedBy")
    val completedBy: Long,
    @SerializedName("completedAt")
    val completedAt: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("Mission")
    val Mission: Mission,
)

data class Mission(
    val id: Long,
    val title: String,
    val description: String
)