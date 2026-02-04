package com.d104.pnt.base

import android.app.Application
import android.content.Context
import com.d104.pnt.BuildConfig
import com.d104.pnt.util.AuthEventBus
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import timber.log.Timber

/**
 * Application 클래스
 */
@HiltAndroidApp
class BaseApplication : Application() {


    @Inject
    lateinit var authEventBus: AuthEventBus

    companion object {
        private var instance: BaseApplication? = null

        /**
         * Application Context를 반환합니다
         */
        fun getContext(): Context {
            return instance?.applicationContext
                ?: throw IllegalStateException("Application not initialized")
        }
    }


    override fun onCreate() {
        super.onCreate()
        instance = this

        // Timber 초기화
        Timber.plant(Timber.DebugTree())

        // 카카오 SDK 초기화 (BuildConfig 사용)
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        Timber.d("Kakao SDK initialized with key: ${BuildConfig.KAKAO_NATIVE_APP_KEY}")
    }
}