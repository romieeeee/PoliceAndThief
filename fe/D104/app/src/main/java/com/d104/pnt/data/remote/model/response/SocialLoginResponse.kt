package com.d104.pnt.data.remote.model.response

import com.google.gson.annotations.SerializedName

data class SocialLoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("member")
    val member: MemberInfo
) {
    data class MemberInfo(
        @SerializedName("memberId")
        val memberId: Long,

        @SerializedName("id")
        val id: String,

        @SerializedName("nickname")
        val nickname: String,

        @SerializedName("avatarUrl")
        val avatarUrl: String?,

        @SerializedName("role")
        val role: String
    )
}