package com.d104.pnt.ui.chatroom.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.ChatRoomResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.ChatRepository
import com.d104.pnt.data.repository.ProfileRepository
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.ChatSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import com.d104.pnt.domain.model.common.UiState

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val chatSocketManager: ChatSocketManager,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val chatRoomId: Long = savedStateHandle.get<Long>(NavArgs.CHAT_ID) ?: 0
    private val _roomInfo = MutableStateFlow<ChatRoomResponse?>(null)
    val roomInfo: StateFlow<ChatRoomResponse?> = _roomInfo.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    // 메시지 리스트: id 오름차순 정렬 (1, 2, 3, ... 65)
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val myMemberId = MutableStateFlow<Long>(0)

    private val _members = MutableStateFlow<List<ChatRoomMemberUi>>(emptyList())
    val members: StateFlow<List<ChatRoomMemberUi>> = _members.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uiState: StateFlow<UiState<String>> = _uiState.asStateFlow()

    private val _selectedProfile = MutableStateFlow<ProfileData?>(null)
    val selectedProfile: StateFlow<ProfileData?> = _selectedProfile.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    init {
        Timber.d("ChatRoomViewModel 초기화 - chatRoomId: $chatRoomId")

        setupChatCallbacks()
        fetchRoomInfo()

        viewModelScope.launch {
            authRepository.getMemberId().collect { id ->
                myMemberId.value = id
                Timber.d("내 멤버 ID: $id")
            }
        }

        connectToChatRoom()
    }

    private fun fetchRoomInfo() {
        viewModelScope.launch {
            when (val result = chatRepository.getChatRoomInfo(chatRoomId)) {
                is BaseResult.Success -> _roomInfo.value = result.data
                is BaseResult.Error -> Timber.e("채팅방 정보 로드 실패: ${result.error.message}")
            }
        }
    }

    private fun connectToChatRoom() {
        viewModelScope.launch {
            authRepository.getAccessToken().collect { token ->
                if (token.isNotEmpty()) {
                    chatSocketManager.connect(token)

                    if (chatSocketManager.getCurrentChatRoomId() == chatRoomId) {
                        loadInitialMessages()
                    } else {
                        chatSocketManager.joinRoom(chatRoomId) { success, message ->
                            if (success) {
                                Timber.d("채팅방 입장 성공: $message")
                                viewModelScope.launch {
                                    // REST join — 백엔드 멤버 등록
                                    chatRepository.joinChatRoom(chatRoomId)
                                }
                                loadInitialMessages()
                            } else {
                                Timber.e("채팅방 입장 실패: $message")
                            }
                        }
                    }
                }
                return@collect
            }
        }
    }

    private fun setupChatCallbacks() {
        // 새 메시지 수신
        chatSocketManager.setOnNewMessage { data ->
            val newMessage = parseMessage(data) ?: return@setOnNewMessage
            viewModelScope.launch {
                if (_chatMessages.value.none { it.id == newMessage.id }) {
                    _chatMessages.value = (_chatMessages.value + newMessage).sortedBy { it.id }
                    Timber.d("✅ 새 메시지 추가: id=${newMessage.id}")
                }
            }
        }

        // 이전 메시지 조회 (초기 로드 + 페이징)
        chatSocketManager.setOnPreviousMessages { messages, count ->
            Timber.d("📥 get prev chat: ${count}개 메시지 수신")
            val parsedMessages = messages.mapNotNull { parseMessage(it) }

            viewModelScope.launch {
                val currentIds = _chatMessages.value.map { it.id }.toSet()
                val newMessages = parsedMessages.filter { it.id !in currentIds }

                if (newMessages.isNotEmpty()) {
                    _chatMessages.value = (_chatMessages.value + newMessages).sortedBy { it.id }
                    Timber.d("✅ 이전 메시지 추가: ${newMessages.size}개 (전체: ${_chatMessages.value.size}개)")
                } else {
                    Timber.d("⚠️ 새로운 메시지 없음")
                }

                _isLoading.value = false
            }
        }

        // 동기화 메시지 (재연결 시)
        chatSocketManager.setOnSyncMessages { messages, count ->
            Timber.d("📥 get sync chat: ${count}개 메시지 수신")
            val parsedMessages = messages.mapNotNull { parseMessage(it) }

            viewModelScope.launch {
                val currentIds = _chatMessages.value.map { it.id }.toSet()
                val newMessages = parsedMessages.filter { it.id !in currentIds }

                if (newMessages.isNotEmpty()) {
                    _chatMessages.value = (_chatMessages.value + newMessages).sortedBy { it.id }
                    Timber.d("✅ 동기화 메시지 추가: ${newMessages.size}개")
                }
            }
        }

        // 재연결 처리
        chatSocketManager.setOnReconnected { reconnectedRoomId ->
            Timber.d("🔄 재입장 완료: chatRoomId=$reconnectedRoomId")

            viewModelScope.launch {
                val lastMessageId = _chatMessages.value.lastOrNull()?.id

                if (lastMessageId != null && lastMessageId > 0) {
                    Timber.d("📤 post sync chat 요청: cursor=$lastMessageId")
                    chatSocketManager.syncMessages(lastMessageId)
                } else {
                    Timber.d("메시지 없음 - 초기 로드")
                    loadInitialMessages()
                }
            }
        }

        // 방장 위임 결과 수신
        chatSocketManager.setOnOwnerDelegated { data ->
            val roomId = data.optLong("roomId")
            val newOwnerId = data.optLong("newOwnerId")

            Timber.d("✅ 방장 위임 완료: roomId=$roomId, newOwnerId=$newOwnerId")

            viewModelScope.launch {
                // 모든 클라이언트가 멤버 목록 갱신
                loadMembers()

                // 내가 새 방장이 되었다면 알림
                if (newOwnerId == myMemberId.value) {
                    _uiState.value = UiState.Success("방장이 되었습니다")
                }
            }
        }

        // 강퇴 이벤트 수신
        chatSocketManager.setOnMemberKicked { data ->
            val chatRoomId = data.optLong("chatRoomId")
            val kickedMemberId = data.optLong("kickMemberId")

            Timber.d("📢 강퇴 이벤트: chatRoomId=$chatRoomId, kickedMemberId=$kickedMemberId")

            viewModelScope.launch {
                if (kickedMemberId == myMemberId.value) {
                    Timber.e("❌ 본인이 강퇴당함 - 연결 종료")

                    chatSocketManager.disconnect()

                    _uiState.value = UiState.Error("채팅방에서 강퇴되었습니다")

                } else {
                    Timber.d("다른 멤버 강퇴됨 - 목록 갱신")
                    loadMembers()
                }
            }
        }
    }

    private fun parseMessage(data: JSONObject): ChatMessage? {
        return try {
            ChatMessage(
                id = data.getInt("id"),
                chatRoomId = chatRoomId,
                memberId = data.optLong("senderId", data.optLong("memberId", 0)),
                senderNickname = data.optString("senderNickname", "익명"),
                avatarUrl = data.optString("avatarUrl", ""),
                content = data.getString("content"),
            )
        } catch (e: Exception) {
            Timber.e(e, "메시지 파싱 실패: $data")
            null
        }
    }

    private fun loadInitialMessages() {
        _isLoading.value = true
        Timber.d("📤 post prev chat: cursor=null (초기 로드)")
        chatSocketManager.loadPreviousMessages(cursor = -1, limit = 50)
    }

    fun loadMoreMessages() {
        if (_isLoading.value) {
            Timber.d("⚠️ 이미 로딩 중")
            return
        }

        val oldestMessageId = _chatMessages.value.firstOrNull()?.id
        Timber.d("📜 loadMoreMessages - oldestMessageId: $oldestMessageId, 현재: ${_chatMessages.value.size}개")

        if (oldestMessageId != null && oldestMessageId > 0) {
            _isLoading.value = true
            Timber.d("📤 post prev chat: cursor=$oldestMessageId")
            chatSocketManager.loadPreviousMessages(cursor = oldestMessageId, limit = 50)
        }
    }

    fun writeMessage(newMessage: String) {
        _message.value = newMessage
    }

    fun sendMessage() {
        val messageText = _message.value.trim()
        if (messageText.isEmpty()) return

        Timber.d("메시지 전송: $messageText")
        chatSocketManager.sendMessage(messageText)
        _message.value = ""
    }

    /**
     * 우측 드로어에서 호출 (멤버 목록 갱신)
     */
    fun loadMembers() {
        viewModelScope.launch {
            when (val result = chatRepository.getChatRoomMembers(chatRoomId)) {
                is BaseResult.Success -> {
                    _members.value = result.data.map { m ->
                        ChatRoomMemberUi(
                            memberId = m.memberId,
                            nickname = m.nickname,
//                            avatarUrl = m.avatarUrl,
                            isHost = m.owner
                        )
                    }
                }
                is BaseResult.Error -> {
                    Timber.e("채팅방 멤버 목록 로드 실패: ${result.error.message}")
                }
            }
        }
    }

    fun leaveRoom(
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = chatRepository.leaveChatRoom(chatRoomId)) {
                is BaseResult.Success -> {
                    Timber.d("채팅방 나가기 성공")
                    onSuccess()
                }
                is BaseResult.Error -> {
                    Timber.e("채팅방 나가기 실패: ${result.error.message}")
                }
            }
        }
    }

    fun disconnectRoom() {
        // viewModelScope가 취소되어도 이 블록은 끝까지 실행됨
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                Timber.d("🚀 서버에 disconnect 요청 중...")
                chatRepository.disconnectChatRoom(chatRoomId)
                chatSocketManager.disconnect() // 소켓도 여기서 같이 끊어줘 행님!
                Timber.d("✅ 모든 정리 작업 완료")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        disconnectRoom()
        Timber.d("ChatRoomViewModel cleared")
    }

    // 방장 위임
    fun delegateHost(targetMemberId: Long) {
        Timber.d("방장 위임 요청: targetMemberId=$targetMemberId")
        chatSocketManager.delegateOwner(targetMemberId)
    }

    // 강퇴하기
    fun kickMember(targetMemberId: Long, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // REST API 호출
            when (val result = chatRepository.kickChatRoomMember(chatRoomId, targetMemberId, reason)) {
                is BaseResult.Success -> {
                    Timber.d("강퇴 API 호출 성공")

                    chatSocketManager.notifyKickMember(targetMemberId)
                }
                is BaseResult.Error -> {
                    Timber.e("강퇴 실패: ${result.error.message}")
                    _uiState.value = UiState.Error(result.error.message ?: "강퇴 실패")
                }
            }
            _isLoading.value = false
        }
    }

    fun clearUiState() {
        _uiState.value = UiState.Idle
    }

    fun loadUserProfile(memberId: Long) {
        viewModelScope.launch {
            _isProfileLoading.value = true

            when (val result = profileRepository.getMyProfile(memberId)) {
                is BaseResult.Success -> {
                    val profile = result.data
                    _selectedProfile.value = ProfileData(
                        nickname = profile.nickname ?: "알 수 없음",
                        avatarUrl = profile.avatarUrl,
                        policeGrade = profile.stat.policeGrade,
                        thiefGrade = profile.stat.thiefGrade
                    )
                }
                is BaseResult.Error -> {
                    Timber.e("프로필 조회 실패: ${result.error.message}")
                    _selectedProfile.value = null
                }
            }

            _isProfileLoading.value = false
        }
    }

    fun clearSelectedProfile() {
        _selectedProfile.value = null
    }
}

data class ProfileData(
    val nickname: String,
    val avatarUrl: String?,
    val policeGrade: String,
    val thiefGrade: String
)