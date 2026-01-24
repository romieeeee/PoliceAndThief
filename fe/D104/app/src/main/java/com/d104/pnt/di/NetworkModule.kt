package com.d104.pnt.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.d104.pnt.base.AddCookiesInterceptor
import com.d104.pnt.base.Constants
import com.d104.pnt.base.ReceivedCookiesInterceptor
import com.d104.pnt.data.remote.api.AuthApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network 관련 의존성 제공 모듈
 *
 * 제공하는 의존성:
 * - Gson
 * - SharedPreferences (쿠키 저장용)
 * - HttpLoggingInterceptor
 * - AddCookiesInterceptor (Hilt로 주입)
 * - ReceivedCookiesInterceptor (Hilt로 주입)
 * - AuthInterceptor (Authorization Header)
 * - OkHttpClient
 * - Retrofit
 * - ApiService...
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Gson 제공
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    /**
     * SharedPreferences 제공 (쿠키 저장용)
     *
     * 이 SharedPreferences는:
     * - AddCookiesInterceptor에 주입됨
     * - ReceivedCookiesInterceptor에 주입됨
     */
    @Provides
    @Singleton
    fun provideCookiePreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("cookie_prefs", Context.MODE_PRIVATE)
    }

    /**
     * HTTP 로깅 인터셉터 제공
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Authorization Header 자동 추가 인터셉터
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        dataStore: DataStore<Preferences>
    ): Interceptor {
        return Interceptor { chain ->
            val token = runBlocking {
                dataStore.data.first()[stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)] ?: ""
            }

            val request = if (token.isNotEmpty()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
    }

    /**
     * OkHttpClient 제공
     *
     * Hilt가 자동으로:
     * 1. AddCookiesInterceptor 생성 (cookiePreferences 주입)
     * 2. ReceivedCookiesInterceptor 생성 (cookiePreferences 주입)
     * 3. OkHttpClient에 전달
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        addCookiesInterceptor: AddCookiesInterceptor,      // ← Hilt가 자동 생성!
        receivedCookiesInterceptor: ReceivedCookiesInterceptor  // ← Hilt가 자동 생성!
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(addCookiesInterceptor)        // 쿠키 추가
            .addInterceptor(receivedCookiesInterceptor)   // 쿠키 저장
            .addInterceptor(authInterceptor)              // Authorization 헤더
            .build()
    }

    /**
     * Retrofit 제공
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * AuthApiService 제공
     */
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

//    @Provides
//    @Singleton
//    fun provideGameApiService(retrofit: Retrofit): GameApiService {
//        return retrofit.create(GameApiService::class.java)
//    }
}