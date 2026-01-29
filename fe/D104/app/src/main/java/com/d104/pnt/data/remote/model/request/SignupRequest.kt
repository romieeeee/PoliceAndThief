package com.d104.pnt.data.remote.model.request

import com.google.gson.annotations.SerializedName
/**
 * 회원가입 요청
 */
data class SignupRequest(
    @SerializedName("loginId")
    val id: String,

    val password: String,
    val passwordConfirm: String,

    @SerializedName("nickname")
    val nickname: String,
    val email: String,
    val birth: String,
    val avatarUrl: String? = null
)


/**
 * 아이디 중복 체크 요청
 */

data class CheckDuplicateRequest(
    val id: String,
)