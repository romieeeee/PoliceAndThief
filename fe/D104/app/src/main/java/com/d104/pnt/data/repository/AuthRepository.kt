package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.LoginResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.Flow


interface AuthRepository {

    /**
     * 로그인
     */
    suspend fun login(id: String, password: String): BaseResult<LoginResponse>

    /**
     * 회원가입
     */
    suspend fun signup(
        id: String,
        password: String,
        nickname: String,
        avatarUrl: String? = null
    ): BaseResult<LoginResponse>

    /**
     * 로그아웃
     */
    suspend fun logout(): BaseResult<Unit>

    /**
     * 로그인 상태 확인
     * @return Flow<Boolean> (true: 로그인됨, false: 로그아웃됨)
     */
    fun isLoggedIn(): Flow<Boolean>

    /**
     * 로그인 정보 저장
     */
    suspend fun saveLoginData(
        accessToken: String,
        refreshToken: String,
        userId: String
    )

    /**
     * 인증 정보 삭제
     */
    suspend fun clearAuthData()
}