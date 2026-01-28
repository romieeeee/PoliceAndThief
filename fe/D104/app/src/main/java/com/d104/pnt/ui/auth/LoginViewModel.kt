package com.d104.pnt.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.LoginResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.util.KakaoLoginHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    sealed interface LoginEvent {
        data class Success(val userId: String) : LoginEvent
        data class Error(val message: String) : LoginEvent
        object Loading : LoginEvent
    }

    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    // 로그인 상태
    private val _loginState = MutableStateFlow<UiState<LoginResponse>>(UiState.Idle)
    val loginState: StateFlow<UiState<LoginResponse>> = _loginState.asStateFlow()

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
     * 카카오 소셜 로그인
     */
    /**
     * 카카오 소셜 로그인
     */
    fun loginWithKakao(context: Context) {
        viewModelScope.launch {
            _loginEvent.emit(LoginEvent.Loading)

            try {
                // 1. 카카오 SDK로 액세스 토큰 받기
                val kakaoToken = KakaoLoginHelper.login(context)

                if (kakaoToken == null) {
                    Timber.w("Kakao login cancelled or failed")
                    _loginEvent.emit(LoginEvent.Error("카카오 로그인을 취소했습니다"))
                    return@launch
                }

                Timber.d("Kakao token received: ${kakaoToken.take(10)}...")

                // 2. 서버에 provider="kakao"와 token 전송
                when (val result = authRepository.socialLogin("KAKAO", kakaoToken)) {
                    is BaseResult.Success -> {
                        Timber.d("Social login successful: ${result.data.member.id}")
                        _loginEvent.emit(LoginEvent.Success(result.data.member.id))
                    }

                    is BaseResult.Error -> {
                        Timber.e("Social login failed: ${result.error.message}")
                        _loginEvent.emit(LoginEvent.Error("로그인 실패: ${result.error.message}"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during Kakao login")
                _loginEvent.emit(LoginEvent.Error("로그인 중 오류가 발생했습니다"))
            }
        }
    }

    /**
     * State 초기화 (화면 이동 후 사용)
     */
    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }
}