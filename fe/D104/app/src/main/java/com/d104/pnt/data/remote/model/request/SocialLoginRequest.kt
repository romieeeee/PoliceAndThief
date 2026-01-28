package com.d104.pnt.data.remote.model.request

import com.google.gson.annotations.SerializedName

data class SocialLoginRequest(
    @SerializedName("provider")
    val provider: String, // "kakao", "google"

    @SerializedName("token")
    val token: String // 소셜 로그인에서 받은 액세스 토큰
)