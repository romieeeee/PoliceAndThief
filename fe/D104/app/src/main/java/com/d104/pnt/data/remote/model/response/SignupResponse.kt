package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class SignupResponse(
    @SerializedName("memberId")
    val memberId: Long,

    @SerializedName("id")
    val id: String,

    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("createdAt")
    val createdAt: String
)

data class DuplicateCheckResponse(
    @SerializedName("duplicated")
    val duplicated: Boolean
)