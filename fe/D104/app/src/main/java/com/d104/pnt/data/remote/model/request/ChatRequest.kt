package com.d104.pnt.data.remote.model.request

import com.google.gson.annotations.SerializedName

data class ChatCreateRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("regionCode")
    val regionCode: Long,
    @SerializedName("maxMembers")
    val maxMembers: Int
)
