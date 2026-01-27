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

    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Timber 초기화
        Timber.plant(Timber.DebugTree())

        Timber.d("BaseApplication with Hilt initialized")
    }
}