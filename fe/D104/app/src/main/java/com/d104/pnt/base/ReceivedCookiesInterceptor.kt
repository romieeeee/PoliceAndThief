package com.d104.pnt.base

import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

/**
 * 서버로부터 받은 쿠키를 SharedPreferences에 저장하는 Interceptor
 *
 * 동작 방식:
 * 1. 응답 헤더에서 "Set-Cookie" 추출
 * 2. SharedPreferences에 쿠키 저장
 * 3. 이후 요청에서 자동으로 쿠키 사용
 */
class ReceivedCookiesInterceptor @Inject constructor(
    private val cookiePreferences: SharedPreferences
) : Interceptor {

    companion object {
        private const val COOKIE_KEY = "cookies"
    }

    /**
     * HTTP 응답을 가로채서 Set-Cookie 헤더를 추출하여 저장합니다.
     * @param chain Interceptor Chain
     * @return HTTP Response
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        // 응답 헤더에서 Set-Cookie 추출
        val setCookieHeaders = originalResponse.headers("Set-Cookie")

        if (setCookieHeaders.isNotEmpty()) {
            val cookies = setCookieHeaders.toSet()

            // SharedPreferences에 쿠키 저장
            cookiePreferences
                .edit()
                .putStringSet(COOKIE_KEY, cookies)
                .apply()

            Timber.d("Cookies saved: ${cookies.size} items")
        }

        return originalResponse
    }
}