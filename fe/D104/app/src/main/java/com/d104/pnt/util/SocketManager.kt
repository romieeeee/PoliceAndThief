package com.d104.pnt.util

import com.d104.pnt.base.Constants
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor() {
    var socket: Socket? = null
    private var currentChatRoomId: Long? = null

    companion object {
        private const val EVENT_POST_JOIN_ROOM = "post join room"  // 송신
        private const val EVENT_GET_JOIN_ROOM = "get join room"   // 수신

        private const val EVENT_DISCONNECT = "disconnect"
        private const val EVENT_RECONNECT = "reconnect"

        private const val EVENT_POST_MESSAGE = "post message"
        private const val EVENT_GET_MESSAGE = "get message"

        private const val EVENT_POST_PREV_CHAT = "post prev chat"
        private const val EVENT_GET_PREV_CHAT = "get prev chat"

        private const val EVENT_POST_SYNC_CHAT = "post sync chat"
        private const val EVENT_GET_SYNC_CHAT = "get sync chat"

        private const val EVENT_ERROR = "error"
    }

    fun connect(authToken: String) {
        if (socket?.connected() == true) {
            Timber.d("이미 연결되어 있습니다.")
            return
        }

        try {
            val socketUrl = "${Constants.BASE_URL}chat"
            Timber.d("소켓 연결 시도: $socketUrl")
            Timber.d("토큰: ${authToken.take(20)}...")

            // 토큰 설정
            val options = IO.Options().apply {
                auth = mapOf("token" to authToken)
            }

            socket = IO.socket(socketUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Timber.d("✅ 서버에 연결되었습니다.")
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Timber.d("❌ 서버 연결이 끊어졌습니다: ${args.firstOrNull()}")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Timber.e("🔴 연결 에러: ${args.firstOrNull()}")
            }

            socket?.on("reconnect") {
                Timber.d("🔄 재연결됨")
                currentChatRoomId?.let { roomId ->
                    handleReconnect(roomId)
                }
            }

            // 전역 에러 리스너
            socket?.on(EVENT_ERROR) { args ->
                try {
                    val error = args[0] as JSONObject
                    val message = error.getString("message")
                    val code = error.getInt("code")
                    Timber.e("❌ Socket Error: $message (code: $code)")
                } catch (e: Exception) {
                    Timber.e(e, "에러 파싱 실패")
                }
            }

            socket?.connect()

        } catch (e: Exception) {
            Timber.e(e, "소켓 연결 실패")
        }
    }

    fun joinRoom(chatRoomId: Long, onResponse: (Boolean, String) -> Unit) {
        currentChatRoomId = chatRoomId

        val data = JSONObject().apply {
            put("chatRoomId", chatRoomId)
        }

        Timber.d("📤 채팅방 입장 요청: $data")

        socket?.emit(EVENT_POST_JOIN_ROOM, data)

        socket?.once(EVENT_GET_JOIN_ROOM) { args ->
            try {
                val response = args[0] as JSONObject

                val message = response.optString("message", "")
                val roomId = response.optInt("chatRoomId", 0)

                Timber.d("📥 채팅방 입장: message=$message, chatRoomId=$roomId")

                // message가 "joined room"이면 성공!
                if (message == "joined room" && roomId > 0) {
                    onResponse(true, "채팅방에 입장하였습니다.")
                } else {
                    onResponse(false, message)
                }

            } catch (e: Exception) {
                Timber.e(e, "입장 응답 파싱 실패")
                onResponse(false, "응답 파싱 실패: ${e.message}")
            }
        }
    }

    private fun handleReconnect(chatRoomId: Long) {
        val data = JSONObject().apply {
            put("chatRoomId", chatRoomId)
        }

        Timber.d("🔄 재연결 - 채팅방 재입장: $data")
        socket?.emit(EVENT_RECONNECT, data)
    }

    // 채팅 메시지 보내기
    fun sendChatMessage(content: String) {
        val data = JSONObject().apply {
            put("content", content)
        }

        Timber.d("💬 채팅 메시지 전송: $data")
        socket?.emit(EVENT_POST_MESSAGE, data)
    }

    // 새 메시지 수신 리스너
    fun onNewMessage(handler: (JSONObject) -> Unit) {
        socket?.on(EVENT_GET_MESSAGE) { args ->
            try {
                val data = args[0] as JSONObject
                Timber.d("💬 새 메시지 수신: $data")
                handler(data)
            } catch (e: Exception) {
                Timber.e(e, "메시지 파싱 실패")
            }
        }
    }

    // 이전 메시지 조회 (페이징)
    fun loadPreviousMessages(cursor: Int, limit: Int = 50) {
        val data = JSONObject().apply {
            put("cursor", cursor)
            put("limit", limit)
        }

        Timber.d("📜 이전 메시지 조회: $data")
        socket?.emit(EVENT_POST_PREV_CHAT, data)
    }

    // 이전 메시지 수신 리스너
    fun onPreviousMessages(handler: (List<JSONObject>, Int) -> Unit) {
        socket?.on(EVENT_GET_PREV_CHAT) { args ->
            try {
                val response = args[0] as JSONObject
                val items = response.getJSONArray("items")
                val count = response.getInt("count")

                val messageList = mutableListOf<JSONObject>()
                for (i in 0 until items.length()) {
                    messageList.add(items.getJSONObject(i))
                }

                Timber.d("📜 이전 메시지 수신: ${count}개")
                handler(messageList, count)
            } catch (e: Exception) {
                Timber.e(e, "이전 메시지 파싱 실패")
            }
        }
    }

    // 동기화 (재연결 시 놓친 메시지)
    fun syncMessages(lastMessageId: Int) {
        val data = JSONObject().apply {
            put("cursor", lastMessageId)
        }

        Timber.d("🔄 메시지 동기화: $data")
        socket?.emit(EVENT_POST_SYNC_CHAT, data)
    }

    // 동기화 메시지 수신 리스너
    fun onSyncMessages(handler: (List<JSONObject>, Int) -> Unit) {
        socket?.on(EVENT_GET_SYNC_CHAT) { args ->
            try {
                val response = args[0] as JSONObject
                val items = response.getJSONArray("items")
                val count = response.getInt("count")

                val messageList = mutableListOf<JSONObject>()
                for (i in 0 until items.length()) {
                    messageList.add(items.getJSONObject(i))
                }

                Timber.d("🔄 동기화 메시지 수신: ${count}개")
                handler(messageList, count)
            } catch (e: Exception) {
                Timber.e(e, "동기화 메시지 파싱 실패")
            }
        }
    }

    fun emit(event: String, data: Any) {
        socket?.emit(event, data)
        Timber.d("📤 이벤트 전송: $event")
    }

    fun on(event: String, handler: (Array<Any>) -> Unit) {
        socket?.on(event) { args ->
            Timber.d("📥 이벤트 수신: $event")
            handler(args)
        }
    }

    // 🔥 메시지 리스너 전체 해제
    fun removeMessageListeners() {
        socket?.off(EVENT_GET_MESSAGE)
        socket?.off(EVENT_GET_PREV_CHAT)
        socket?.off(EVENT_GET_SYNC_CHAT)
        Timber.d("🔕 메시지 리스너 해제")
    }

    fun disconnect() {
        Timber.d("🔌 소켓 연결 종료")
        socket?.emit(EVENT_DISCONNECT)
        socket?.disconnect()
        socket?.off()
        currentChatRoomId = null
    }

    fun isConnected(): Boolean = socket?.connected() ?: false
}