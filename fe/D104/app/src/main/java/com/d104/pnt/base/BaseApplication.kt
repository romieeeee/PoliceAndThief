package com.d104.pnt.base

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Application 클래스
 */
@HiltAndroidApp
class BaseApplication : Application() {

    companion object {
        private var instance: BaseApplication? = null

        /**
         * Application Context를 반환합니다
         */
        fun getContext(): Context {
            return instance?.applicationContext
                ?: throw IllegalStateException("Application not initialized")
        }

        // Notification Channel IDs
        const val CHANNEL_LOCATION_SERVICE = "location_service_channel"
        const val CHANNEL_GAME_ALERT = "game_alert_channel"
        const val CHANNEL_BOUNDARY_WARNING = "boundary_warning_channel"

        // ===== DataStore 유틸리티 메서드 =====
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = Constants.PREF_NAME
        )

        private fun getDataStore(): DataStore<Preferences> {
            return getContext().dataStore
        }

        // DataStore Keys
        private val KEY_ACCESS_TOKEN = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
        private val KEY_REFRESH_TOKEN = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
        private val KEY_USER_ID = stringPreferencesKey(Constants.KEY_USER_ID)
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey(Constants.KEY_IS_LOGGED_IN)

        /**
         * Access Token을 Flow로 반환 (Compose에서 사용)
         */
        fun getAccessTokenFlow(): Flow<String> {
            return getDataStore().data.map { preferences ->
                preferences[KEY_ACCESS_TOKEN] ?: ""
            }
        }

        /**
         * Access Token을 동기적으로 가져오기 (Interceptor 전용)
         * 주의: 메인 스레드에서 사용하지 마세요!
         */
        fun getAccessTokenSync(): String {
            return runBlocking {
                getDataStore().data.first()[KEY_ACCESS_TOKEN] ?: ""
            }
        }

        /**
         * 로그인 정보를 한 번에 저장
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
         * 로그아웃 시 모든 인증 정보 삭제
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

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Timber 초기화
        Timber.plant(Timber.DebugTree())

        Timber.d("BaseApplication with Hilt initialized")
    }
}