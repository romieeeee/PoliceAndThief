package com.d104.pnt.ui.chatroom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.ChatRepository
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.SocketManager
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
    private val socketManager: SocketManager,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatRoomId: Long = savedStateHandle.get<Long>(NavArgs.CHAT_ID) ?: 0

    // UI에 보여줄 채팅방 정보 상태
    private val _roomInfo =
        MutableStateFlow<com.d104.pnt.data.remote.model.response.ChatRoomResponse?>(null)
    val roomInfo: StateFlow<com.d104.pnt.data.remote.model.response.ChatRoomResponse?> =
        _roomInfo.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 내 멤버 ID
    val myMemberId = MutableStateFlow<Long>(0)

    init {
        Timber.d("ChatRoomViewModel 초기화 - chatRoomId: $chatRoomId")

        fetchRoomInfo()

        viewModelScope.launch {
            authRepository.getMemberId().collect { id ->
                myMemberId.value = id
                Timber.d("내 멤버 ID: $id")
            }
        }

        // 메시지 리스너 설정
        setupMessageListeners()

        // 초기 메시지 로드
        loadInitialMessages()
    }

    private fun fetchRoomInfo() {
        viewModelScope.launch {
            val result = chatRepository.getChatRoomInfo(chatRoomId)

            when (result) {
                is BaseResult.Success -> {
                    val info = result.data
                    _roomInfo.value = info
                    Timber.d("채팅방 정보 로드 성공: ${info.title}")
                }
                is BaseResult.Error -> {
                    val apiError = result.error
                    Timber.e("채팅방 정보 로드 실패: ${apiError.message} (코드: ${apiError.code})")
                }
            }
        }
    }


    // 메시지 리스너 설정
    private fun setupMessageListeners() {
        // 새 메시지 수신
        socketManager.onNewMessage { data ->
            val newMessage = parseMessage(data)
            if (newMessage != null) {
                viewModelScope.launch {
                    _chatMessages.value = _chatMessages.value + newMessage
                    Timber.d("💬 새 메시지 추가: ${newMessage.content}")
                }
            }
        }

        // 이전 메시지 조회 결과
        socketManager.onPreviousMessages { messages, count ->
            val parsedMessages = messages.mapNotNull { parseMessage(it) }
            viewModelScope.launch {
                // 기존 메시지 앞에 추가 (시간순 정렬)
                _chatMessages.value = parsedMessages + _chatMessages.value
                _isLoading.value = false
                Timber.d("📜 이전 메시지 로드 완료: ${count}개")
            }
        }

        // 동기화 메시지 (재연결 시)
        socketManager.onSyncMessages { messages, count ->
            val parsedMessages = messages.mapNotNull { parseMessage(it) }
            viewModelScope.launch {
                _chatMessages.value = _chatMessages.value + parsedMessages
                Timber.d("🔄 동기화 메시지 추가: ${count}개")
            }
        }
    }

    // JSON → ChatMessage 변환
    private fun parseMessage(data: JSONObject): ChatMessage? {
        return try {
            ChatMessage(
                id = data.getInt("id"),
                chatRoomId = data.getInt("chatRoomId"),
                memberId = data.getLong("memberId"),
                senderNickname = data.getString("senderNickname"),
                avataUrl = data.optString("avataUrl", ""),
                content = data.getString("content"),
            )
        } catch (e: Exception) {
            Timber.e(e, "메시지 파싱 실패: $data")
            null
        }
    }

    // 초기 메시지 로드 (채팅방 진입 시)
    private fun loadInitialMessages() {
        _isLoading.value = true
        // cursor 0 = 최신 메시지부터, limit 50개
        socketManager.loadPreviousMessages(cursor = 0, limit = 50)
    }

    // 더 오래된 메시지 로드 (스크롤 시)
    fun loadMoreMessages() {
        if (_isLoading.value) return

        val oldestMessageId = _chatMessages.value.firstOrNull()?.id ?: 0
        if (oldestMessageId > 0) {
            _isLoading.value = true
            socketManager.loadPreviousMessages(cursor = oldestMessageId, limit = 50)
        }
    }

    fun writeMessage(newMessage: String) {
        _message.value = newMessage
    }

    // 메시지 전송
    fun sendMessage() {
        val messageText = _message.value.trim()

        if (messageText.isEmpty()) {
            Timber.d("빈 메시지는 전송하지 않음")
            return
        }

        Timber.d("💬 메시지 전송: $messageText")

        socketManager.sendChatMessage(messageText)

        // 입력창 초기화
        _message.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.removeMessageListeners()
        Timber.d("ChatRoomViewModel cleared")
    }
}