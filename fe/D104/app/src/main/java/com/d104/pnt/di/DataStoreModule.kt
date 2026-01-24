package com.d104.pnt.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.d104.pnt.base.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore 의존성 제공 모듈
 *
 * 제공하는 의존성:
 * - DataStore<Preferences> (사용자 인증 정보 저장소)
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    // DataStore 인스턴스 생성 (Extension property)
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = Constants.PREF_NAME
    )

    /**
     * DataStore 제공
     *
     * Repository나 다른 클래스에서 주입받아 사용
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}