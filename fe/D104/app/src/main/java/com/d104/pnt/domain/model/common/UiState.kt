package com.d104.pnt.domain.model.common

/**
 * UI 상태를 나타내는 Sealed Class
 * 사용 예시:
 * private val _loginState = MutableStateFlow<UiState<LoginResponse>>(UiState.Idle)
 *
 * when (loginState) {
 *     is UiState.Idle -> // 초기 상태
 *     is UiState.Loading -> // 로딩 중
 *     is UiState.Success -> // 성공
 *     is UiState.Error -> // 에러
 * }
 */
sealed class UiState<out T> {
    /**
     * 초기 상태
     */
    data object Idle : UiState<Nothing>()

    /**
     * 로딩 중
     */
    data object Loading : UiState<Nothing>()

    /**
     * 성공
     * @param data 성공 시 받은 데이터
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * 에러
     * @param message 에러 메시지
     */
    data class Error(val message: String) : UiState<Nothing>()
}