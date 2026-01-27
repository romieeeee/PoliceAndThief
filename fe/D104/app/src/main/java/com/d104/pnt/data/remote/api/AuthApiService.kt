package com.d104.pnt.data.remote.api

import com.d104.pnt.data.remote.model.response.BaseResponse
import com.d104.pnt.data.remote.model.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    @GET("api/test")
    suspend fun test(): Response<BaseResponse<Unit>>


    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<BaseResponse<LoginResponse>>

    /**
     * 회원가입 API
     */
    @POST("api/auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<BaseResponse<LoginResponse>>

    /**
     * 로그아웃 API
     */
    @POST("api/auth/logout")
    suspend fun logout(): Response<BaseResponse<Unit>>
}


/**
 * 로그인 요청
 */
data class LoginRequest(
    val id: String,
    val password: String
)

/**
 * 회원가입 요청
 */
data class SignupRequest(
    val id: String,
    val password: String,
    val nickname: String,
    val avatarUrl: String? = null
)