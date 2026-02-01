package com.d104.pnt.util.socket

import com.d104.pnt.base.Constants
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import timber.log.Timber

/**
 * 모든 소켓 매니저의 기본 클래스
 * - 연결, 재연결, 에러 처리, 이벤트 등록/해제
 */
abstract class BaseSocketManager(
    private val namespace: String
) {
    protected var socket: Socket? = null
    private var isManualDisconnect = false

    companion object {
        const val EVENT_RECONNECT = "reconnect"
        const val EVENT_ERROR = "error"
    }

    /**
     * 소켓 연결
     */
    fun connect(authToken: String) {
        if (socket != null && (socket!!.connected() || !isManualDisconnect)) {
            Timber.d("[$namespace] 소켓이 이미 활성화 상태이거나 연결 시도 중입니다.")
            return
        }

        socket?.let {
            Timber.d("[$namespace] 기존 소켓 인스턴스 정리")
            it.off()
            it.disconnect()
            it.close() // 완전히 파괴
        }
        socket = null

        try {
            val socketUrl = "${Constants.BASE_URL}$namespace"
            Timber.d("[$namespace] 소켓 연결 시도: $socketUrl")

            val options = IO.Options().apply {
                auth = mapOf("token" to authToken)
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                forceNew = true
                transports = arrayOf("websocket")
            }

            socket = IO.socket(socketUrl, options)

            setupBaseListeners()
            setupCustomListeners()

            socket?.connect()

        } catch (e: Exception) {
            Timber.e(e, "[$namespace] 소켓 연결 실패")
        }
    }

    /**
     * 기본 소켓 이벤트 리스너 설정
     */
    private fun setupBaseListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Timber.d("[$namespace] 서버에 연결되었습니다.")
                isManualDisconnect = false
                onConnect()
            }

            on(Socket.EVENT_DISCONNECT) { args ->
                Timber.d("[$namespace] 서버 연결이 끊어졌습니다: ${args.firstOrNull()}")
                onDisconnect()
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                Timber.e("[$namespace] 연결 에러: ${args.firstOrNull()}")
                onError(args.firstOrNull()?.toString() ?: "Unknown error")
            }

            on(EVENT_RECONNECT) { args ->
                try {
                    val data = args[0] as JSONObject
                    Timber.d("[$namespace] 재연결됨: $data")
                    onReconnect(data)
                } catch (e: Exception) {
                    Timber.e(e, "[$namespace] 재연결 데이터 파싱 실패")
                }
            }

            on(EVENT_ERROR) { args ->
                try {
                    val error = args[0] as JSONObject
                    val message = error.getString("message")
                    val code = error.getInt("code")
                    Timber.e("[$namespace] Socket Error: $message (code: $code)")
                    onError("$message (code: $code)")
                } catch (e: Exception) {
                    Timber.e(e, "[$namespace] 에러 파싱 실패")
                }
            }
        }
    }

    /**
     * 커스텀 이벤트 리스너 설정 (하위 클래스에서 구현)
     */
    protected abstract fun setupCustomListeners()

    /**
     * 연결 성공 시 콜백
     */
    protected open fun onConnect() {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * 연결 해제 시 콜백
     */
    protected open fun onDisconnect() {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * 재연결 시 콜백
     */
    protected open fun onReconnect(data: JSONObject) {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * 에러 발생 시 콜백
     */
    protected open fun onError(message: String) {
        // 하위 클래스에서 필요시 오버라이드
    }

    /**
     * 정상 연결 해제
     */
    fun disconnect() {
        Timber.d("[$namespace] 소켓 연결 종료")
        isManualDisconnect = true

        socket?.let {
            if (it.connected()) {
                removeAllListeners()
                it.disconnect()
            }
        }

        socket = null
        onDisconnectCleanup()
    }

    /**
     * 연결 해제 시 정리 작업 (하위 클래스에서 구현)
     */
    protected open fun onDisconnectCleanup() {

    }

    /**
     * 모든 리스너 제거
     */
    protected open fun removeAllListeners() {
        socket?.off()
    }

    /**
     * 연결 상태 확인
     */
    fun isConnected(): Boolean = socket?.connected() ?: false

    /**
     * 이벤트 전송
     */
    protected fun emit(event: String, data: JSONObject) {
        if (!isConnected()) {
            Timber.e("[$namespace] 소켓이 연결되어 있지 않습니다.")
            return
        }
        socket?.emit(event, data)
        Timber.d("[$namespace] 이벤트 전송: $event - $data")
    }

    /**
     * 이벤트 리스너 등록
     */
    protected fun on(event: String, handler: (Array<Any>) -> Unit) {
        socket?.on(event) { args ->
            Timber.d("[$namespace] 이벤트 수신: $event")
            handler(args)
        }
    }

    /**
     * 일회성 이벤트 리스너 등록
     */
    protected fun once(event: String, handler: (Array<Any>) -> Unit) {
        socket?.once(event) { args ->
            Timber.d("[$namespace] 이벤트 수신 (once): $event")
            handler(args)
        }
    }

    /**
     * 이벤트 리스너 제거
     */
    protected fun off(event: String) {
        socket?.off(event)
        Timber.d("[$namespace] 이벤트 리스너 제거: $event")
    }
}