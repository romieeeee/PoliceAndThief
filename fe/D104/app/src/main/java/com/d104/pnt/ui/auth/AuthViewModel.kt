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

/**
 * 인증 관련 ViewModel
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<LoginResponse>>(UiState.Idle)
    val loginState: StateFlow<UiState<LoginResponse>> = _loginState.asStateFlow()

    private val _signupState = MutableStateFlow<UiState<LoginResponse>>(UiState.Idle)
    val signupState: StateFlow<UiState<LoginResponse>> = _signupState.asStateFlow()

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState.asStateFlow()

    // 로그인 여부 (Repository에서 Flow로 제공)
    val isLoggedIn = authRepository.isLoggedIn()

    fun test() {
        viewModelScope.launch {
            when (val result = authRepository.test()) {
                is BaseResult.Success -> {
                    Timber.d("Test: ${result.isSuccess}")
                }

                is BaseResult.Error -> {
                    Timber.e("Test failed: ${result.error.message}")
                }
            }
        }
    }

    // ===== 사용자 액션 =====

    /**
     * 로그인
     */
    fun login(id: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading

            when (val result = authRepository.login(id, password)) {
                is BaseResult.Success -> {
                    _loginState.value = UiState.Success(result.data)
                    Timber.d("Login successful: ${result.data.member.nickname}")
                }

                is BaseResult.Error -> {
                    _loginState.value = UiState.Error(result.error.message)
                    Timber.e("Login failed: ${result.error.message}")
                }
            }
        }
    }

    /**
     * 회원가입
     */
    fun signup(id: String, password: String, nickname: String, avatarUrl: String? = null) {
        viewModelScope.launch {
            _signupState.value = UiState.Loading

            when (val result = authRepository.signup(id, password, nickname, avatarUrl)) {
                is BaseResult.Success -> {
                    _signupState.value = UiState.Success(result.data)
                    Timber.d("Signup successful: ${result.data.member.nickname}")
                }

                is BaseResult.Error -> {
                    _signupState.value = UiState.Error(result.error.message)
                    Timber.e("Signup failed: ${result.error.message}")
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

    fun resetSignupState() {
        _signupState.value = UiState.Idle
    }

    fun resetLogoutState() {
        _logoutState.value = UiState.Idle
    }
}