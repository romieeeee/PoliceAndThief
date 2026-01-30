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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val myMemberId = MutableStateFlow<Long>(0)

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

                    // 이미 입장했는지 체크
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
        // 새 메시지 수신 (중복 체크!)
        chatSocketManager.setOnNewMessage { data ->
            val newMessage = parseMessage(data)
            if (newMessage != null) {
                viewModelScope.launch {
                    if (_chatMessages.value.none { it.id == newMessage.id }) {
                        _chatMessages.value = _chatMessages.value + newMessage
                        Timber.d("✅ 새 메시지 추가: id=${newMessage.id}, ${newMessage.content}")
                    } else {
                        Timber.d("⚠️ 중복 메시지 무시: id=${newMessage.id}")
                    }
                }
            }
        }

        // 이전 메시지 조회 결과 (중복 제거!)
        chatSocketManager.setOnPreviousMessages { messages, count ->
            val parsedMessages = messages.mapNotNull { parseMessage(it) }

            viewModelScope.launch {
                val currentIds = _chatMessages.value.map { it.id }.toSet()
                val newMessages = parsedMessages
                    .filter { it.id !in currentIds }

                _chatMessages.value =
                    (newMessages + _chatMessages.value)
                        .sortedBy { it.id }

                _isLoading.value = false
            }
        }


        // 동기화 메시지 (중복 제거!)
        chatSocketManager.setOnSyncMessages { messages, count ->
            Timber.d("📥 get sync chat: ${count}개 메시지 수신")
            val parsedMessages = messages.mapNotNull { parseMessage(it) }
            viewModelScope.launch {
                val currentIds = _chatMessages.value.map { it.id }.toSet()
                val newMessages = parsedMessages.filter { it.id !in currentIds }

                _chatMessages.value = _chatMessages.value + newMessages
                Timber.d("✅ 동기화 메시지 추가: ${newMessages.size}개 (중복 ${parsedMessages.size - newMessages.size}개 제거)")
            }
        }

        // 재연결 처리
        chatSocketManager.setOnReconnected { reconnectedRoomId ->
            Timber.d("🔄 재입장 완료: chatRoomId=$reconnectedRoomId -> 동기화 시작")

            viewModelScope.launch {
                // 마지막 메시지 ID(Cursor) 추출
                val lastMessageId = _chatMessages.value.lastOrNull()?.id

                if (lastMessageId != null && lastMessageId > 0) {
                    Timber.d("📤 post sync chat 요청: cursor=$lastMessageId")
                    chatSocketManager.syncMessages(lastMessageId)
                } else {
                    // 메시지가 하나도 없었다면 새로 불러오기
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
        Timber.d("📤 post prev chat: cursor=null, limit=50 (초기 메시지 로드)")
        chatSocketManager.loadPreviousMessages(cursor = -1, limit = 50)
    }

    fun loadMoreMessages() {
        if (_isLoading.value) return

        val oldestMessageId = _chatMessages.value.firstOrNull()?.id
        if (oldestMessageId != null && oldestMessageId > 0) {
            _isLoading.value = true
            Timber.d("📤 post prev chat: cursor=$oldestMessageId, limit=50 (더 오래된 메시지)")
            chatSocketManager.loadPreviousMessages(cursor = oldestMessageId, limit = 50)
        }
    }

    fun writeMessage(newMessage: String) {
        _message.value = newMessage
    }

    fun sendMessage() {
        val messageText = _message.value.trim()
        if (messageText.isEmpty()) {
            Timber.d("빈 메시지는 전송하지 않음")
            return
        }

        Timber.d("메시지 전송: $messageText")
        chatSocketManager.sendMessage(messageText)
        _message.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        // chatSocketManager.leaveRoom()  // 재연결 테스트용 주석
        Timber.d("ChatRoomViewModel cleared")
    }
}