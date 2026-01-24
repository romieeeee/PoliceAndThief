package com.d104.pnt.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.d104.pnt.base.Constants
import com.d104.pnt.data.remote.api.AuthApiService
import com.d104.pnt.data.remote.api.LoginRequest
import com.d104.pnt.data.remote.api.SignupRequest
import com.d104.pnt.data.remote.model.response.LoginResponse
import com.d104.pnt.domain.model.common.BaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val dataStore: DataStore<Preferences>
) : AuthRepository, BaseRepository() {
    // DataStore Keys
    private val KEY_ACCESS_TOKEN = stringPreferencesKey(Constants.KEY_ACCESS_TOKEN)
    private val KEY_REFRESH_TOKEN = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
    private val KEY_USER_ID = stringPreferencesKey(Constants.KEY_USER_ID)
    private val KEY_IS_LOGGED_IN = booleanPreferencesKey(Constants.KEY_IS_LOGGED_IN)

    override suspend fun login(id: String, password: String): BaseResult<LoginResponse> {
        return safeApiCall(
            onSuccess = { loginResponse ->
                saveLoginData(
                    accessToken = loginResponse.accessToken,
                    refreshToken = loginResponse.refreshToken,
                    userId = loginResponse.member.id
                )
            }
        ) {
            apiService.login(LoginRequest(id, password))
        }
    }

    override suspend fun signup(
        id: String,
        password: String,
        nickname: String,
        avatarUrl: String?
    ): BaseResult<LoginResponse> {
        return safeApiCall(
            onSuccess = { loginResponse ->
                saveLoginData(
                    accessToken = loginResponse.accessToken,
                    refreshToken = loginResponse.refreshToken,
                    userId = loginResponse.member.id
                )
            }
        ) {
            apiService.signup(SignupRequest(id, password, nickname, avatarUrl))
        }
    }

    override suspend fun logout(): BaseResult<Unit> {
        return safeApiCall(
            onSuccess = { clearAuthData() }
        ) {
            apiService.logout()
        }
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_IS_LOGGED_IN] ?: false
        }
    }

    override suspend fun saveLoginData(
        accessToken: String,
        refreshToken: String,
        userId: String
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
            preferences[KEY_REFRESH_TOKEN] = refreshToken
            preferences[KEY_USER_ID] = userId
            preferences[KEY_IS_LOGGED_IN] = true
        }
        Timber.d("Login data saved for user: $userId")
    }

    override suspend fun clearAuthData() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_REFRESH_TOKEN)
            preferences.remove(KEY_USER_ID)
            preferences[KEY_IS_LOGGED_IN] = false
        }
        Timber.d("Auth data cleared")
    }
}