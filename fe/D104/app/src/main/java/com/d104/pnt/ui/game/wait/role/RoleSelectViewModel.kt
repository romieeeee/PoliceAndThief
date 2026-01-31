package com.d104.pnt.ui.game.wait.role

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.RoomSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RoleSelectViewModel @Inject constructor(
    private val gameRoomRepository: GameRoomRepository,
    private val authRepository: AuthRepository,
    private val roomSocketManager: RoomSocketManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val roomId: Long = savedStateHandle.get<Long>(NavArgs.ROOM_ID) ?: 0L

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    init {
        connect()
    }

    private fun connect() {
        viewModelScope.launch {
            authRepository.getAccessToken().collect { token ->
                if (token.isNotEmpty()) {
                    roomSocketManager.currentRoomId = roomId
                    roomSocketManager.connect(token)

                    // 연결될 때까지 대기 후 방 입장 신호(Join) 전송
                    while (!roomSocketManager.isConnected()) {
                        delay(100)
                    }

                    roomSocketManager.joinRoom(roomId) { _, _ ->
                        Timber.d("🌐 역할 선택 화면에서 소켓 선연결 및 Join 완료")
                    }
                }
            }
        }
    }

    fun selectRole(role: GameRole, onSuccess: (GameRole) -> Unit) {
        viewModelScope.launch {
            val roleName = if (role == GameRole.ANY) "THIEF" else role.name
            // HTTP로 DB 먼저 수정
            val result = gameRoomRepository.changePosition(roomId, roleName)
            if (result is BaseResult.Success) {
                // 소켓으로 다른 사람들에게 내 역할 알림
                roomSocketManager.updatePosition(roleName)
                onSuccess(role)
            }
        }
    }
}