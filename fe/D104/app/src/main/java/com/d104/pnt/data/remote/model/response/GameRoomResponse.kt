package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class CreateGameRoomResponse(
    @SerializedName("roomId")
    val roomId: Long,
    @SerializedName("roomCode")
    val roomCode: String,
    @SerializedName("status")
    val status: String,
)
