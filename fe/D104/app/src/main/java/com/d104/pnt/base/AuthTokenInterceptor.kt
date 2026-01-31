package com.d104.pnt.base

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.d104.pnt.data.remote.api.AuthApiService
import com.d104.pnt.data.remote.model.request.RefreshRequest
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.util.AuthEventBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

/**
 * JWT Token을 요청 헤더에 추가하는 Interceptor
 */
class AuthTokenInterceptor @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authEventBus: AuthEventBus,
    private val authAPiServiceProvider: Provider<AuthApiService>,
    private val authRepositoryProvider: Provider<AuthRepository>
) : Interceptor {

    companion object {
        // 토큰이 필요 없는 URL 패턴
        private val PUBLIC_URLS = listOf(
            "/auth/login",
            "/auth/social-login",
            "/auth/signup",
            "/auth/duplicate",
            "/auth/reissue"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val noAuthHeader = request.header("X-No-Auth")

        // 공개 URL이면 토큰 추가하지 않음
        if (PUBLIC_URLS.any { path.contains(it) }) {
            Timber.d("Public URL, skipping token: $path")
            return chain.proceed(request)
        }

        if (noAuthHeader != null) {
            val newRequest = request.newBuilder()
                .removeHeader("X-No-Auth")
                .build()
            return chain.proceed(newRequest)
        }

        // 토큰 읽기
        val token = runBlocking {
            dataStore.data.first()[stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)]
        }

        // 토큰이 있으면 추가
        val newRequest = if (!token.isNullOrEmpty()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            Timber.w("No token found for protected URL: $path")
            request
        }

        if (!token.isNullOrEmpty()) {
            Timber.d("Added JWT token to request: $path")
        }

        val response = chain.proceed(newRequest)

        // 401 에러 (토큰 만료) 처리
        if (response.code == 401) {
            Timber.w("Access token expired (401) - Refresh")
            if (path.contains("/auth/reissue")) {
                Timber.d("refreshToken are expired")
                runBlocking {
                    // 로컬 데이터 삭제
                    clearAuthData()
                    // 토큰 만료 이벤트 발생
                    authEventBus.emit(AuthEventBus.AuthEvent.TokenExpired)
                }
                return response
            }
            response.close()

            synchronized(this) {
                val currentToken = runBlocking { dataStore.data.first()[stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)] }

                if (token != currentToken && !currentToken.isNullOrEmpty()) {
                    val newerRequest = newRequest.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                    return chain.proceed(newerRequest)
                }

                val refreshToken = runBlocking { dataStore.data.first()[stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)] }
                if (refreshToken.isNullOrEmpty()) {
                    Timber.w("No refresh token found, skipping token refresh")
                    runBlocking {
                        // 로컬 데이터 삭제
                        clearAuthData()
                        // 토큰 만료 이벤트 발생
                        authEventBus.emit(AuthEventBus.AuthEvent.TokenExpired)
                    }
                }
                Timber.d("""
                    accessToken = $currentToken
                    refreshToken = $refreshToken
                """)
                try {
                    val refreshCall = authAPiServiceProvider.get().refreshTokenCall(
                        RefreshRequest(
                            grantType = "Bearer",
                            accessToken = currentToken ?: "",
                            refreshToken = refreshToken ?: "",
                            accessTokenExpiresIn = 0,
                            refreshTokenExpiresIn = 0
                        )
                    )
                    val refreshResponse = refreshCall.execute() // 동기 실행

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newTokens = refreshResponse.body()!!

                        // 7. 새 토큰 저장
                        runBlocking {
                            authRepositoryProvider.get().refreshTokens(newTokens.data!!.accessToken, newTokens.data.refreshToken)
                        }

                        Timber.d("✅ 토큰 갱신 성공! 재요청 진행")

                        // 8. 원래 요청에 새 토큰 갈아끼우고 재전송 (Retry)
                        val newerRequest = newRequest.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.data!!.accessToken}")
                            .build()

                        return chain.proceed(newerRequest)
                    } else {
                        // 갱신 실패 (Refresh Token도 만료됨 등)
                        Timber.e("토큰 갱신 실패 (서버 응답 오류)")
                        runBlocking {
                            // 로컬 데이터 삭제
                            clearAuthData()
                            // 토큰 만료 이벤트 발생
                            authEventBus.emit(AuthEventBus.AuthEvent.TokenExpired)
                        }
                        return chain.proceed(newRequest) // 401 반환
                    }
                } catch (e: Exception) {
                    Timber.e(e, "토큰 갱신 중 네트워크 오류")
                    runBlocking {
                        // 로컬 데이터 삭제
                        clearAuthData()
                        // 토큰 만료 이벤트 발생
                        authEventBus.emit(AuthEventBus.AuthEvent.TokenExpired)
                    }
                    return chain.proceed(newRequest)
                }
            }
        }

        return response
    }

    /**
     * 인증 데이터 삭제
     */
    private suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(Constants.KEY_ACCESS_TOKEN))
            preferences.remove(stringPreferencesKey(Constants.KEY_REFRESH_TOKEN))
            preferences.remove(stringPreferencesKey(Constants.KEY_USER_ID))
            preferences.remove(longPreferencesKey(Constants.KEY_MEMBER_ID))
            preferences[booleanPreferencesKey(Constants.KEY_IS_LOGGED_IN)] = false
        }
        Timber.d("Auth data cleared due to token expiration")
    }
}