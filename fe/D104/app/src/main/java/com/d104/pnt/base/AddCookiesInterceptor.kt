package com.d104.pnt.base

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
* SharedPreferences에 저장된 쿠키를 요청 헤더에 추가하는 Interceptor
*
* 동작 방식:
* 1. SharedPreferences에서 저장된 쿠키 조회
* 2. 요청 헤더에 "Cookie" 추가
* 3. 서버에 전송
*/
class AddCookiesInterceptor : Interceptor {

    companion object {
        private const val COOKIE_KEY = "cookies"
    }

    /**
     * HTTP 요청에 저장된 쿠키를 추가합니다.
     * @param chain Interceptor Chain
     * @return HTTP Response
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()

        // SharedPreferences에서 쿠키 조회 (동기 처리)
        val cookies = BaseApplication.cookiePreferences
            .getStringSet(COOKIE_KEY, emptySet()) ?: emptySet()

        // 쿠키를 요청 헤더에 추가
        cookies.forEach { cookie ->
            builder.addHeader("Cookie", cookie)
        }

        if (cookies.isNotEmpty()) {
            Timber.d("Added ${cookies.size} cookies to request")
        }

        return chain.proceed(builder.build())
    }
}