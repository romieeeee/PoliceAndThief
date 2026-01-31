package com.d104.pnt.data.repository

import com.d104.pnt.data.remote.model.response.DuplicateCheckResponse
import com.d104.pnt.data.remote.model.response.LoginResponse
import com.d104.pnt.data.remote.model.response.SignupResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.Flow


interface AuthRepository {

    /**
     * 로그인
     */
    suspend fun login(id: String, password: String): BaseResult<LoginResponse>


    /**
     * 소셜 로그인
     */
    suspend fun socialLogin(provider: String, token: String): BaseResult<LoginResponse>


    /**
     * 로그아웃
     */
    suspend fun logout(): BaseResult<String>

    /**
     * 로그인 상태 확인
     * @return Flow<Boolean> (true: 로그인됨, false: 로그아웃됨)
     */
    fun isLoggedIn(): Flow<Boolean>

    /**
     * DataStore 읽기
     */
    fun getUserId(): Flow<String>
    fun getMemberId(): Flow<Long>
    suspend fun getUserIdSync(): String
    fun getAccessToken(): Flow<String>
    fun getRefreshToken(): Flow<String>

    /**
     * 로그인 정보 저장
     */
    suspend fun saveLoginData(
        accessToken: String,
        refreshToken: String,
        userId: String,
        memberId: Long
    )

    suspend fun refreshTokens(
        accessToken: String,
        refreshToken: String
    )

    /**
     * 인증 정보 삭제
     */
    suspend fun clearAuthData()


    /**
     * ID 중복 체크
     */
    suspend fun checkDuplicate(id: String): BaseResult<DuplicateCheckResponse>

    /**
     * 회원가입
     */
    suspend fun signup(
        id: String,
        password: String,
        passwordConfirm: String,
        nickname: String,
        email: String,
        birth: String,
        avatarUrl: String? = null
    ): BaseResult<SignupResponse>

}