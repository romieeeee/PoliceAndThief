package com.d104.pnt.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.LoginResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _id = MutableStateFlow("")
    val id: StateFlow<String> = _id.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    // 로그인 상태
    private val _loginState = MutableStateFlow<UiState<LoginResponse>>(UiState.Idle)
    val loginState: StateFlow<UiState<LoginResponse>> = _loginState.asStateFlow()

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState.asStateFlow()

    // 로그인 여부 (Repository에서 Flow로 제공)
    val isLoggedIn = authRepository.isLoggedIn()

    // 입력값 업데이트
    fun updateId(newId: String) {
        _id.value = newId
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword
    }

    // 입력 유효성 검사
    fun isLoginEnabled(): Boolean {
        return id.value.isNotBlank() && password.value.length >= 4
    }

    // 로그인
    fun login() {
        viewModelScope.launch {
            _loginState.value = UiState.Loading

            when (val result = authRepository.login(id.value, password.value)) {
                is BaseResult.Success -> {
                    Timber.d("${result.data.member}")
                    _loginState.value = UiState.Success(result.data)
                }

                is BaseResult.Error -> {
                    Timber.e("code: ${result.error.code} msg: ${result.error.message}")
                    _loginState.value = UiState.Error(result.error.message)
                }
            }
        }
    }


    /**
     * 로그아웃
     */
    fun logout() {
        viewModelScope.launch {
            _logoutState.value = UiState.Loading

            when (val result = authRepository.logout()) {
                is BaseResult.Success -> {
                    _logoutState.value = UiState.Success(Unit)
                    Timber.d("Logout successful")
                }

                is BaseResult.Error -> {
                    _logoutState.value = UiState.Error(result.error.message)
                    Timber.e("Logout failed: ${result.error.message}")
                }
            }
        }
    }

    /**
     * State 초기화 (화면 이동 후 사용)
     */
    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    fun resetLogoutState() {
        _logoutState.value = UiState.Idle
    }
}