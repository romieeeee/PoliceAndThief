package com.d104.pnt.util

import com.d104.pnt.base.Constants
import io.socket.client.IO
import io.socket.client.Socket
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor() {
    private var socket: Socket? = null

    fun connect() {
        if (socket?.connected() == true) return

        try {
            socket = IO.socket(Constants.BASE_URL)

            // 기본 연결 이벤트 리스너
            socket?.on(Socket.EVENT_CONNECT) {
                Timber.d("서버에 연결되었습니다.")
            }

            socket?.connect()
        } catch (e: Exception) {
            Timber.e(e, "소켓 연결 실패")
        }
    }

    // 서버에 메시지 보내기 (Emit)
    fun emit(event: String, data: Any) {
        socket?.emit(event, data)
    }

    // 서버 메시지 받기 (On)
    fun on(event: String, handler: (Array<Any>) -> Unit) {
        socket?.on(event) { args ->
            handler(args)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
    }
}