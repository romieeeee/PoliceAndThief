package com.example.d104.base

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Application 클래스
 * 앱의 전역 상태 및 공통 설정을 관리
 *
 * 주요 기능:
 * - Retrofit 인스턴스 생성 및 관리
 * - DataStore를 통한 인증 데이터 저장/조회
 * - SharedPreferences를 통한 쿠키 저장 (Interceptor 동기 처리용)
 * - Notification 채널 생성
 */
class BaseApplication : Application() {

    companion object {
        private var instance: BaseApplication? = null

        /**
         * Application Context를 반환합니다.
         * @return Application Context
         * @throws IllegalStateException Application이 초기화되지 않은 경우
         */
        fun getContext(): Context {
            return instance?.applicationContext
                ?: throw IllegalStateException("Application not initialized")
        }

        // DataStore
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = Constants.PREF_NAME
        )

        fun getDataStore(): DataStore<Preferences> {
            return getContext().dataStore
        }

        // DataStore Keys
        private val KEY_ACCESS_TOKEN = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
        private val KEY_REFRESH_TOKEN = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
        private val KEY_USER_ID = stringPreferencesKey(Constants.KEY_USER_ID)
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey(Constants.KEY_IS_LOGGED_IN)

        // SharedPreferences (Cookie 저장 전용)
        lateinit var cookiePreferences: SharedPreferences
            private set

        /**
         * Retrofit 인스턴스
         */
        lateinit var retrofit: Retrofit
            private set

        val gson: Gson = GsonBuilder()
            .setLenient() // JSON 파싱을 유연하게 처리
            .create()

        // 권한 배열
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,      // 정확한 위치
                Manifest.permission.ACCESS_COARSE_LOCATION,    // 대략적인 위치
                Manifest.permission.CAMERA,                     // 카메라 (미션 촬영)
                Manifest.permission.RECORD_AUDIO,               // 오디오 녹음 (무전기)
                Manifest.permission.POST_NOTIFICATIONS          // 알림 (Android 13+)
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        }

        // Notification Channel IDs
        const val CHANNEL_LOCATION_SERVICE = "location_service_channel"
        const val CHANNEL_GAME_ALERT = "game_alert_channel"
        const val CHANNEL_BOUNDARY_WARNING = "boundary_warning_channel"

        // DataStore 유틸리티 메서드
        /**
         * Access Token을 Flow로 반환합니다. (Compose에서 collectAsState로 사용)
         */
        fun getAccessTokenFlow(): Flow<String> {
            return getDataStore().data.map { preferences ->
                preferences[KEY_ACCESS_TOKEN] ?: ""
            }
        }

        /**
         * Refresh Token을 Flow로 반환합니다.
         */
        fun getRefreshTokenFlow(): Flow<String> {
            return getDataStore().data.map { preferences ->
                preferences[KEY_REFRESH_TOKEN] ?: ""
            }
        }

        /**
         * User ID를 Flow로 반환합니다.
         */
        fun getUserIdFlow(): Flow<String> {
            return getDataStore().data.map { preferences ->
                preferences[KEY_USER_ID] ?: ""
            }
        }

        /**
         * 로그인 상태를 Flow로 반환합니다.
         */
        fun getIsLoggedInFlow(): Flow<Boolean> {
            return getDataStore().data.map { preferences ->
                preferences[KEY_IS_LOGGED_IN] ?: false
            }
        }

        /**
         * Access Token을 동기적으로 가져옵니다. (Interceptor 전용)
         * 주의: 메인 스레드에서 사용하지 마세요!
         */
        fun getAccessTokenSync(): String {
            return runBlocking {
                getDataStore().data.first()[KEY_ACCESS_TOKEN] ?: ""
            }
        }

        /**
         * Access Token을 DataStore에 저장합니다.
         * @param token 저장할 토큰
         */
        suspend fun saveAccessToken(token: String) {
            getDataStore().edit { preferences ->
                preferences[KEY_ACCESS_TOKEN] = token
            }
        }

        /**
         * Refresh Token을 DataStore에 저장합니다.
         * @param token 저장할 리프레시 토큰
         */
        suspend fun saveRefreshToken(token: String) {
            getDataStore().edit { preferences ->
                preferences[KEY_REFRESH_TOKEN] = token
            }
        }

        /**
         * User ID를 DataStore에 저장합니다.
         * @param userId 저장할 사용자 ID
         */
        suspend fun saveUserId(userId: String) {
            getDataStore().edit { preferences ->
                preferences[KEY_USER_ID] = userId
            }
        }

        /**
         * 로그인 상태를 DataStore에 저장합니다.
         * @param isLoggedIn 로그인 여부
         */
        suspend fun saveLoginStatus(isLoggedIn: Boolean) {
            getDataStore().edit { preferences ->
                preferences[KEY_IS_LOGGED_IN] = isLoggedIn
            }
        }

        /**
         * 로그인 정보를 한 번에 저장합니다.
         * @param accessToken Access Token
         * @param refreshToken Refresh Token
         * @param userId User ID
         */
        suspend fun saveLoginData(accessToken: String, refreshToken: String, userId: String) {
            getDataStore().edit { preferences ->
                preferences[KEY_ACCESS_TOKEN] = accessToken
                preferences[KEY_REFRESH_TOKEN] = refreshToken
                preferences[KEY_USER_ID] = userId
                preferences[KEY_IS_LOGGED_IN] = true
            }
        }

        /**
         * 로그아웃 시 모든 인증 정보를 삭제합니다.
         */
        suspend fun clearAuthData() {
            getDataStore().edit { preferences ->
                preferences.remove(KEY_ACCESS_TOKEN)
                preferences.remove(KEY_REFRESH_TOKEN)
                preferences.remove(KEY_USER_ID)
                preferences[KEY_IS_LOGGED_IN] = false
            }
        }
    }

    /**
     * Application 생성 시 호출되는 메서드
     * 앱의 전역 초기화를 수행합니다.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this

        // SharedPreferences 초기화 (쿠키 저장 전용)
        cookiePreferences = getSharedPreferences("cookie_prefs", MODE_PRIVATE)

        // Retrofit 초기화
        initRetrofit()

        // 알림 채널 생성 (Android 8.0 이상)
//        createNotificationChannels()

        Timber.d("BaseApplication initialized successfully")
    }

    /**
     * Retrofit 인스턴스를 초기화합니다.
     */
    private fun initRetrofit() {
        // HTTP 로깅 인터셉터 (Debug 모드에서만 상세 로그 출력)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            HttpLoggingInterceptor.Level.BODY  // 요청/응답 본문까지 모두 로깅
//                HttpLoggingInterceptor.Level.NONE  // Release에서는 로깅 비활성화
        }

        // OkHttp 클라이언트 생성
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)           // HTTP 로깅
            .addInterceptor(AddCookiesInterceptor())      // 쿠키 추가
            .addInterceptor(ReceivedCookiesInterceptor()) // 쿠키 저장
            .addInterceptor { chain ->
                // Authorization Header 자동 추가 (DataStore에서 토큰 가져오기)
                val token = getAccessTokenSync()
                val request = if (token.isNotEmpty()) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()

        // Retrofit 인스턴스 생성
        retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        Timber.d("Retrofit initialized with baseUrl: ${Constants.BASE_URL}")
    }

    // TODO: 알림 채널 생성...

}