package com.d104.pnt.util

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * 카카오 로그인 헬퍼
 */
object KakaoLoginHelper {

    /**
     * 카카오 로그인
     * @return 카카오 ID 토큰 (성공) 또는 null (실패/취소)
     */
    suspend fun login(context: Context): String? = suspendCancellableCoroutine { continuation ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Timber.e(error, "카카오 로그인 실패")
                continuation.resume(null)
            } else if (token != null) {
                // ID Token 우선, 없으면 Access Token 사용
                val tokenToSend = token.accessToken

                Timber.d("""
                    카카오 로그인 성공!!: 
                    Access Token = ${token.accessToken}
                    ID Token = ${token.idToken}
                    Using: ${if (token.idToken != null) "ID Token" else "Access Token"}
                """.trimIndent())

                continuation.resume(tokenToSend)
            } else {
                continuation.resume(null)
            }
        }

        // 카카오톡 설치 확인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            // 카카오톡으로 로그인
            Timber.d("카카오톡 앱으로 로그인 시도")
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Timber.e(error, "카카오톡 로그인 실패")

                    // 사용자가 취소한 경우
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        Timber.d("사용자가 카카오톡 로그인을 취소함")
                        continuation.resume(null)
                        return@loginWithKakaoTalk
                    }

                    // 카카오톡 로그인 실패 -> 카카오 계정으로 로그인 시도
                    Timber.d("카카오 계정으로 로그인 재시도")
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                } else if (token != null) {
                    Timber.d("카카오톡 로그인 성공")
                    // ⭐ ID Token 우선
                    val tokenToSend = token.idToken ?: token.accessToken
                    continuation.resume(tokenToSend)
                } else {
                    continuation.resume(null)
                }
            }
        } else {
            // 카카오 계정으로 로그인
            Timber.d("카카오 계정으로 로그인 시도")
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }

    /**
     * 카카오 로그아웃
     */
    suspend fun logout(): Boolean = suspendCancellableCoroutine { continuation ->
        UserApiClient.instance.logout { error ->
            if (error != null) {
                Timber.e(error, "카카오 로그아웃 실패")
                continuation.resume(false)
            } else {
                Timber.d("카카오 로그아웃 성공")
                continuation.resume(true)
            }
        }
    }

    /**
     * 카카오 연결 해제 (회원 탈퇴 시 사용)
     */
    suspend fun unlink(): Boolean = suspendCancellableCoroutine { continuation ->
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Timber.e(error, "카카오 연결 해제 실패")
                continuation.resume(false)
            } else {
                Timber.d("카카오 연결 해제 성공")
                continuation.resume(true)
            }
        }
    }

    /**
     * 현재 로그인된 사용자 정보 가져오기 (선택사항)
     */
    suspend fun getUserInfo(): Map<String, String?>? = suspendCancellableCoroutine { continuation ->
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Timber.e(error, "카카오 사용자 정보 가져오기 실패")
                continuation.resume(null)
            } else if (user != null) {
                val userInfo = mapOf(
                    "id" to user.id.toString(),
                    "nickname" to user.kakaoAccount?.profile?.nickname,
                    "email" to user.kakaoAccount?.email,
                    "profileImage" to user.kakaoAccount?.profile?.profileImageUrl
                )
                Timber.d("카카오 사용자 정보: $userInfo")
                continuation.resume(userInfo)
            } else {
                continuation.resume(null)
            }
        }
    }
}