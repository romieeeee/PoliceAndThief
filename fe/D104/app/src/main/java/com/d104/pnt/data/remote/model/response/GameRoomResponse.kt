package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

/**
 * 채팅방 생성 응답
 */
data class CreateGameRoomResponse(
    @SerializedName("roomId")
    val roomId: Long,
    @SerializedName("roomCode")
    val roomCode: String,
    @SerializedName("status")
    val status: String,
)
