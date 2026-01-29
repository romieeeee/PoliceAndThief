package com.d104.pnt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.util.AuthEventBus
import com.d104.pnt.util.SocketManager
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
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val socketManager: SocketManager,
    private val authEventBus: AuthEventBus
) : ViewModel() {
    private val _joinCode = MutableStateFlow("")
    val joinCode: StateFlow<String> = _joinCode.asStateFlow()


    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    fun updateJoinCode(newCode: String) {
        _joinCode.value = newCode
    }


    init {
        viewModelScope.launch {
            authRepository.getAccessToken().collect { token ->
                if (token.isNotEmpty()) {
                    Timber.d("토큰 받음, 소켓 연결 시도")
                    connectSocket(token)
                } else {
                    Timber.e("토큰이 없어서 소켓 연결 실패")
                }
            }
        }
    }


    private fun connectSocket(token: String) {
        try {
            socketManager.connect(token)
        } catch (e: Exception) {
            Timber.e(e, "소켓 연결 실패")
        }
    }


    fun logout() {
        viewModelScope.launch {
            authRepository.clearAuthData()

            Timber.d("Logout completed")
            _uiEvent.emit(HomeUiEvent.ShowMessage("로그아웃되었습니다"))
            _uiEvent.emit(HomeUiEvent.NavigateToIntro)
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect() // 앱 종료 시 소켓 정리
    }
}


sealed interface HomeUiEvent {
    object NavigateToIntro : HomeUiEvent
    data class ShowMessage(val message: String) : HomeUiEvent
    data class ShowError(val message: String) : HomeUiEvent
}



