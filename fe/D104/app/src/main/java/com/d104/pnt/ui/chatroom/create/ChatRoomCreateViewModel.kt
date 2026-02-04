package com.d104.pnt.ui.chatroom.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.ChatRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.GeoLocationInfo
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.util.getSingleLocation
import com.d104.pnt.util.socket.ChatSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatRoomCreateViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val chatRepository: ChatRepository,
    private val chatSocketManager: ChatSocketManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _maxMember = MutableStateFlow(100)
    val maxMember: StateFlow<Int> = _maxMember.asStateFlow()

    private val _currentAddress = MutableStateFlow(GeoLocationInfo("현재 위치", "찾는중..."))
    val currentAddress: StateFlow<GeoLocationInfo> = _currentAddress.asStateFlow()

    private val _createChatRoomStats = MutableStateFlow<UiState<ChatCreateResponse>>(UiState.Idle)
    val createChatRoomStats: StateFlow<UiState<ChatCreateResponse>> =
        _createChatRoomStats.asStateFlow()

    private val _joinRoomState = MutableStateFlow<JoinRoomState>(JoinRoomState.Idle)
    val joinRoomState: StateFlow<JoinRoomState> = _joinRoomState.asStateFlow()

    sealed class JoinRoomState {
        object Idle : JoinRoomState()
        object Loading : JoinRoomState()
        data class Success(val chatRoomId: Long, val message: String) : JoinRoomState()
        data class Error(val message: String) : JoinRoomState()
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateDescription(newDescription: String) {
        _description.value = newDescription
    }

    fun updateMaxMember(plus: Boolean) {
        if (plus && maxMember.value < 200) {
            _maxMember.value += 1
        } else if (!plus && maxMember.value > 50) {
            _maxMember.value -= 1
        }
    }

    /**
     * 위치 정보를 가져오면서 주소도 같이 업데이트
     */
    fun getLocationInfo(context: Context) {
        viewModelScope.launch {
            val location = context.getSingleLocation()
            if (location != null) {
                // 좌표 -> 주소 변환 호출
                val address = locationRepository.getAddressFromLatLng(
                    location.latitude,
                    location.longitude
                )
                _currentAddress.value = address
            }
            if (_currentAddress.value.major != "세종특별자치시") {
                _currentAddress.value = GeoLocationInfo(
                    major = _currentAddress.value.major,
                    middle = _currentAddress.value.middle,
                    code = _currentAddress.value.code / 10000 * 10000
                )
            }
        }
    }

    /**
     * 입력 유효성 검사
     */
    fun isValid(): Boolean {
        if (title.value.isEmpty()) {
            return false
        }
        if (description.value.isEmpty()) {
            return false
        }
        return true
    }

    /**
     * 채팅방 생성
     */
    fun createChatRoom() {
        viewModelScope.launch {
            _createChatRoomStats.value = UiState.Loading

            // 채팅방 생성 API 호출
            when (val result = chatRepository.createChatRoom(
                title.value,
                _currentAddress.value.code,
                description.value,
                maxMember.value
            )) {
                is BaseResult.Success -> {
                    _createChatRoomStats.value = UiState.Success(result.data)
                    val chatRoomId = result.data.chatRoomId
                    Timber.d("채팅방 생성 성공: $chatRoomId")

                    // 참여 → 연결 → 소켓 입장
                    joinChatRoomSequence(chatRoomId)
                }

                is BaseResult.Error -> {
                    _createChatRoomStats.value = UiState.Error(result.error.message)
                    Timber.e("채팅방 생성 실패: ${result.error.message}")
                }
            }
        }
    }

    /**
     * 채팅방 참여 → 연결 → 소켓 입장 순차 처리
     */
    private fun joinChatRoomSequence(chatRoomId: Long) {
        viewModelScope.launch {
            _joinRoomState.value = JoinRoomState.Loading

            // HTTP 참여
            Timber.d("HTTP 참여 시도: $chatRoomId")
            when (val joinResult = chatRepository.joinChatRoom(chatRoomId)) {
                is BaseResult.Success -> {
                    Timber.d("HTTP 참여 성공")

                    // HTTP 연결
                    Timber.d("HTTP 연결 시도: $chatRoomId")
                    when (val connectResult = chatRepository.connectChatRoom(chatRoomId)) {
                        is BaseResult.Success -> {
                            Timber.d("HTTP 연결 성공")

                            // 소켓 입장
                            joinChatRoomViaSocket(chatRoomId)
                        }

                        is BaseResult.Error -> {
                            Timber.e("HTTP 연결 실패: ${connectResult.error.message}")
                            _joinRoomState.value = JoinRoomState.Error(connectResult.error.message)
                        }
                    }
                }

                is BaseResult.Error -> {
                    Timber.e("HTTP 참여 실패: ${joinResult.error.message}")
                    _joinRoomState.value = JoinRoomState.Error(joinResult.error.message)
                }
            }
        }
    }

    /**
     * 소켓을 통한 채팅방 입장
     */
    private fun joinChatRoomViaSocket(chatRoomId: Long) {
        Timber.d("소켓 입장 시도: $chatRoomId")

        viewModelScope.launch {
            // 토큰 확보
            val token = authRepository.getAccessToken().first()
            if (token.isEmpty()) {
                Timber.e("❌ 토큰 없음")
                _joinRoomState.value = JoinRoomState.Error("인증 정보가 없습니다.")
                return@launch
            }

            // 소켓 연결 확인 및 대기
            if (!chatSocketManager.isConnected()) {
                Timber.d("🔌 소켓 연결 시도...")
                chatSocketManager.connect(token)

                var retryCount = 0
                while (!chatSocketManager.isConnected() && retryCount < 50) {
                    delay(100)
                    retryCount++
                }
            }

            if (chatSocketManager.isConnected()) {
                Timber.d("✅ 소켓 연결됨. 입장 요청 전송.")
                joinRoomInternal(chatRoomId)
            } else {
                Timber.e("❌ 소켓 연결 타임아웃 (5초 초과)")
                _joinRoomState.value = JoinRoomState.Error("채팅 서버 연결에 실패했습니다.")
            }
        }
    }

    /**
     * 실제 채팅방 소켓 입장 처리
     */
    private fun joinRoomInternal(chatRoomId: Long) {
        chatSocketManager.joinRoom(chatRoomId) { success, message ->
            if (success) {
                Timber.d("소켓 입장 성공: $message")
                _joinRoomState.value = JoinRoomState.Success(chatRoomId, message)
            } else {
                Timber.e("소켓 입장 실패: $message")
                _joinRoomState.value = JoinRoomState.Error(message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 소켓 연결은 유지 (다음 화면에서 사용할 수 있음)
        Timber.d("ChatRoomCreateViewModel cleared")
    }
}