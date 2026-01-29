package com.d104.pnt.data.remote.model.request

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("avatarUrl")
    val avatarUrl: String
)