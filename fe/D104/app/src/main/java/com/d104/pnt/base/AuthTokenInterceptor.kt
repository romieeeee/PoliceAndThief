package com.d104.pnt.base

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

/**
 * JWT Token을 요청 헤더에 추가하는 Interceptor
 *
 * DataStore에서 토큰을 읽어 Authorization 헤더에 추가
 */
class AuthTokenInterceptor @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // DataStore에서 토큰 동기적으로 읽기
        val token = runBlocking {
            dataStore.data.first()[stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)]
        }

        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        if (!token.isNullOrEmpty()) {
            Timber.d("Added JWT token to request")
        }

        return chain.proceed(request)
    }
}