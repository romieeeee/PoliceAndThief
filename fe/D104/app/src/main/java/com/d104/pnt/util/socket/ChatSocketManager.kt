package com.d104.pnt.util.socket

import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat 네임스페이스 소켓 매니저
 * 채팅 관련 이벤트 처리
 */
@Singleton
class ChatSocketManager @Inject constructor() : BaseSocketManager("chat") {

    private var currentChatRoomId: Long? = null
    fun getCurrentChatRoomId(): Long? = currentChatRoomId

    companion object {
        // Request Events (req)
        private const val EVENT_POST_JOIN_ROOM = "post join room"
        private const val EVENT_POST_MESSAGE = "post message"
        private const val EVENT_POST_PREV_CHAT = "post prev chat"
        private const val EVENT_POST_SYNC_CHAT = "post sync chat"
        private const val EVENT_POST_DISCONNECT = "post disconnect"

        // Response Events (res)
        private const val EVENT_GET_JOIN_ROOM = "get join room"
        private const val EVENT_GET_MESSAGE = "get message"
        private const val EVENT_GET_PREV_CHAT = "get prev chat"
        private const val EVENT_GET_SYNC_CHAT = "get sync chat"
    }

    // Callbacks
    private var onJoinedRoom: ((Long, String) -> Unit)? = null
    private var onNewMessage: ((JSONObject) -> Unit)? = null
    private var onPreviousMessages: ((List<JSONObject>, Int) -> Unit)? = null
    private var onSyncMessages: ((List<JSONObject>, Int) -> Unit)? = null
    private var onReconnected: ((Long) -> Unit)? = null

    override fun setupCustomListeners() {
        // 채팅방 입장 확인
        on(EVENT_GET_JOIN_ROOM) { args ->
            try {
                val response = args[0] as JSONObject
                val message = response.optString("message", "")
                val roomId = response.optLong("chatRoomId", 0)

                Timber.d("채팅방 입장: message=$message, chatRoomId=$roomId")

                if (message == "joined room" && roomId > 0) {
                    onJoinedRoom?.invoke(roomId, "채팅방에 입장하였습니다.")
                } else {
                    onJoinedRoom?.invoke(roomId, message)
                }
            } catch (e: Exception) {
                Timber.e(e, "입장 응답 파싱 실패")
            }
        }

        // 새 메시지 수신
        on(EVENT_GET_MESSAGE) { args ->
            try {
                val data = args[0] as JSONObject
                Timber.d("새 메시지 수신: $data")
                onNewMessage?.invoke(data)
            } catch (e: Exception) {
                Timber.e(e, "메시지 파싱 실패")
            }
        }

        // 이전 메시지 수신
        on(EVENT_GET_PREV_CHAT) { args ->
            try {
                Timber.d("실제 서버 응답: ${args[0]}")

                val messageList = mutableListOf<JSONObject>()
                val items = args[0]

                when (items) {
                    is JSONArray -> {
                        for (i in 0 until items.length()) {
                            messageList.add(items.getJSONObject(i))
                        }
                    }

                    is JSONObject -> {
                        if (items.has("items")) {
                            val innerItems = items.opt("items")
                            if (innerItems is JSONArray) {
                                for (i in 0 until innerItems.length()) {
                                    messageList.add(innerItems.getJSONObject(i))
                                }
                            }
                        }
                    }
                }

                val count = messageList.size
                Timber.d("이전 메시지 수신: ${count}개")
                onPreviousMessages?.invoke(messageList, count)

            } catch (e: Exception) {
                Timber.e(e, "이전 메시지 파싱 실패")
                onPreviousMessages?.invoke(emptyList(), 0)
            }
        }

        // 동기화 메시지 수신
        on(EVENT_GET_SYNC_CHAT) { args ->
            try {
                Timber.d("실제 동기화 응답: ${args[0]}")

                val messageList = mutableListOf<JSONObject>()
                val items = args[0]

                when (items) {
                    is JSONArray -> {
                        for (i in 0 until items.length()) {
                            messageList.add(items.getJSONObject(i))
                        }
                    }

                    is JSONObject -> {
                        if (items.has("items")) {
                            val innerItems = items.opt("items")
                            if (innerItems is JSONArray) {
                                for (i in 0 until innerItems.length()) {
                                    messageList.add(innerItems.getJSONObject(i))
                                }
                            }
                        }
                    }
                }

                val count = messageList.size
                Timber.d("동기화 메시지 수신: ${count}개")
                onSyncMessages?.invoke(messageList, count)

            } catch (e: Exception) {
                Timber.e(e, "동기화 메시지 파싱 실패")
                onSyncMessages?.invoke(emptyList(), 0)
            }
        }
    }

    override fun onReconnect(data: JSONObject) {
        super.onReconnect(data)
        // 서버가 재연결 성공 시 데이터를 준다면 사용하고,
        // 아니라면 기존에 들고 있던 currentChatRoomId를 활용합니다.
        val roomId = data.optLong("chatRoomId", currentChatRoomId ?: 0)

        if (roomId > 0) {
            currentChatRoomId = roomId
            Timber.d("🔄 소켓 재연결 감지 - 방 재입장 시도: chatRoomId=$roomId")

            // 1. 먼저 방에 다시 입장 (서버 세션 복구)
            joinRoom(roomId) { success, message ->
                if (success) {
                    // 2. 입장 성공 후 ViewModel에 재연결 & 동기화 준비 완료 알림
                    onReconnected?.invoke(roomId)
                }
            }
        }
    }

    override fun onDisconnectCleanup() {
        currentChatRoomId = null
        clearCallbacks()
    }

    // ==================== Request Methods ====================

    /**
     * 채팅방 입장
     */
    fun joinRoom(chatRoomId: Long, onResponse: (Boolean, String) -> Unit) {
        currentChatRoomId = chatRoomId

        val data = JSONObject().apply {
            put("chatRoomId", chatRoomId)
        }

        Timber.d("채팅방 입장 요청: $data")
        emit(EVENT_POST_JOIN_ROOM, data)

        // 콜백 등록
        setOnJoinedRoom { roomId, message ->
            if (roomId > 0 && message.contains("입장")) {
                onResponse(true, message)
            } else {
                onResponse(false, message)
            }
        }
    }

    /**
     * 채팅 메시지 전송
     */
    fun sendMessage(content: String, type: String = "TEXT") {
        val roomId = currentChatRoomId ?: run {
            Timber.e("chatRoomId가 없어서 메시지 전송 불가")
            return
        }

        val data = JSONObject().apply {
            put("chatRoomId", roomId)
            put("content", content)
            put("type", type)
        }

        Timber.d("채팅 메시지 전송: $data")
        emit(EVENT_POST_MESSAGE, data)
    }

    /**
     * 이전 메시지 조회 (페이징)
     */
    fun loadPreviousMessages(cursor: Int, limit: Int = 50) {
        val roomId = currentChatRoomId ?: run {
            Timber.e("chatRoomId가 없어서 이전 메시지 조회 불가")
            return
        }

        val data = JSONObject().apply {
            put("cursor", if (cursor == -1) JSONObject.NULL else cursor)
            put("limit", limit)
        }

        Timber.d("이전 메시지 조회: $data")
        emit(EVENT_POST_PREV_CHAT, data)
    }

    /**
     * 메시지 동기화 (재연결 시 놓친 메시지)
     */
    fun syncMessages(cursor: Int) {
        val roomId = currentChatRoomId ?: run {
            Timber.e("chatRoomId가 없어서 메시지 동기화 불가")
            return
        }

        val data = JSONObject().apply {
            put("cursor", cursor)
        }

        Timber.d("메시지 동기화: $data")
        emit(EVENT_POST_SYNC_CHAT, data)
    }


    /**
     * 채팅방 나가기
     */
    fun leaveRoom() {
        if (currentChatRoomId == null) {
            Timber.e("chatRoomId가 없어서 채팅방 나가기 불가")
            return
        }

        Timber.d("채팅방 나가기")

        val data = JSONObject()
        emit(EVENT_POST_DISCONNECT, data)

        disconnect()
    }

    // ==================== Callback Setters ====================

    fun setOnJoinedRoom(callback: (chatRoomId: Long, message: String) -> Unit) {
        onJoinedRoom = callback
    }

    fun setOnNewMessage(callback: (JSONObject) -> Unit) {
        onNewMessage = callback
    }

    fun setOnPreviousMessages(callback: (messages: List<JSONObject>, count: Int) -> Unit) {
        onPreviousMessages = callback
    }

    fun setOnSyncMessages(callback: (messages: List<JSONObject>, count: Int) -> Unit) {
        onSyncMessages = callback
    }

    fun setOnReconnected(callback: (chatRoomId: Long) -> Unit) {
        onReconnected = callback
    }


    private fun clearCallbacks() {
        onJoinedRoom = null
        onNewMessage = null
        onPreviousMessages = null
        onSyncMessages = null
        onReconnected = null
    }

    override fun removeAllListeners() {
        super.removeAllListeners()
        off(EVENT_GET_JOIN_ROOM)
        off(EVENT_GET_MESSAGE)
        off(EVENT_GET_PREV_CHAT)
        off(EVENT_GET_SYNC_CHAT)
    }
}