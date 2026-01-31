package com.d104.pnt.data.remote.model.request

import com.google.gson.annotations.SerializedName

/**
 * 채팅방 생성 요청
 */
data class ChatCreateRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("regionCode")
    val regionCode: Int,
    @SerializedName("maxMembers")
    val maxMembers: Int
)
