package com.d104.pnt.data.remote.model.request

import com.google.gson.annotations.SerializedName

/**
 * 프로필 수정 요청
 */
data class UpdateProfileRequest(
    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("avatarUrl")
    val avatarUrl: String
)