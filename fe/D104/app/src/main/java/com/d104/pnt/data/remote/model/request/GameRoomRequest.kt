package com.d104.pnt.data.remote.model.request

import com.google.gson.annotations.SerializedName

data class CreateGameRoomRequest(
    @SerializedName("playerCount")
    val playerCount: Int,
    @SerializedName("timeLimit")
    val timeLimit: Int,
    @SerializedName("policeCount")
    val policeCount: Int,
    @SerializedName("thiefCount")
    val thiefCount: Int,
    @SerializedName("prison")
    val prison: Location,
    @SerializedName("polygon")
    val polygon: List<Location>
)

data class Location(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

data class ToggleReadyRequest(
    @SerializedName("ready")
    val ready: Boolean
)

data class ChangePositionRequest(
    @SerializedName("preferPosition")
    val preferPosition: String
)

data class JoinGameRoomRequest(
    @SerializedName("roomCode")
    val roomCode: String
)