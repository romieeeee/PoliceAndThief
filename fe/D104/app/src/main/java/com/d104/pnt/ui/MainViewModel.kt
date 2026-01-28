package com.d104.pnt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.util.AuthEventBus
import com.d104.pnt.util.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authEventBus: AuthEventBus,
    private val socketManager: SocketManager
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _message = MutableStateFlow("서버 응답 기다리는 중...")
    val message: StateFlow<String> = _message.asStateFlow()

    init {
        // 1. 소켓 연결 시작
        socketManager.connect()

        // 2. 서버 응답 "듣기" (Step 2)
//        socketManager.on("chat_message") { args ->
//            val receivedData = args[0] as String
//            // 소켓 스레드에서 오기 때문에 StateFlow 업데이트
//            _message.value = receivedData
//        }
    }

    fun sendMessage(text: String) {
        socketManager.emit("test", text)
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect() // 앱 종료 시 소켓 정리
    }
}
