package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class ChatRoomResponse(
    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("currentMembers")
    val currentMembers: Int,

    @SerializedName("deleted")
    val deleted: Boolean,

    @SerializedName("description")
    val description: String,

    @SerializedName("id")
    val chatRoomId: Long,

    @SerializedName("maxMembers")
    val maxMembers: Int,

    @SerializedName("regionCode")
    val regionCode: Int,

    @SerializedName("ownerId")
    val ownerId: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)