package com.d104.pnt.ui.chatroom.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.ChatRoomResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.ChatRepository
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

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val chatSocketManager: ChatSocketManager
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

    // ✅ 멤버 목록(우측 드로어에서 사용)
    private val _members = MutableStateFlow<List<ChatRoomMemberUi>>(emptyList())
    val members: StateFlow<List<ChatRoomMemberUi>> = _members.asStateFlow()

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
                        Timber.d("이미 입장됨 - 메시지만 로드")
                        loadInitialMessages()
                    } else {
                        Timber.d("채팅방 입장 필요")
                        chatSocketManager.joinRoom(chatRoomId) { success, message ->
                            if (success) {
                                Timber.d("채팅방 입장 성공: $message")
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
    }

    private fun parseMessage(data: JSONObject): ChatMessage? {
        return try {
            ChatMessage(
                id = data.getInt("id"),
                chatRoomId = chatRoomId,
                memberId = data.optLong("senderId", data.optLong("memberId", 0)),
                senderNickname = data.optString("senderNickname", "익명"),
                avataUrl = data.optString("avataUrl", ""),
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
}
