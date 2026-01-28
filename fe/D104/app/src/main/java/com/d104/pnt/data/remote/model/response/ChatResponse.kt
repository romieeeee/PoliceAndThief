package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class ChatCreateRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("regionCode")
    val regionCode: Long,
    @SerializedName("maxMember")
    val maxMember: Int
)

data class ChatCreateResponse(
    @SerializedName("chatRoomId")
    val chatRoomId: Long,
    @SerializedName("ownerId")
    val ownerId: Long,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("regionCode")
    val regionCode: Long,
    @SerializedName("maxMember")
    val maxMember: Int,
    @SerializedName("currentMember")
    val currentMember: Int,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("deleted")
    val deleted: Boolean
)
