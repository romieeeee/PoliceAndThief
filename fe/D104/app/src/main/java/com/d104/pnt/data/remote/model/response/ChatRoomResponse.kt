package com.d104.pnt.data.remote.model.response

import com.d104.pnt.data.remote.model.request.Location
import com.google.gson.annotations.SerializedName

/**
 * 채팅방 생성 응답
 */
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

data class MapData(
    @SerializedName("mapId")
    val mapId: Long,
    @SerializedName("mapName")
    val mapName: String,
    @SerializedName("polyPoints")
    val polyPoint: List<Location>,
    @SerializedName("prisonLocation")
    val prisonLocation: Location
)