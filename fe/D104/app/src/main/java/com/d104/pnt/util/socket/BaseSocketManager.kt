package com.d104.pnt.util.socket

import com.d104.pnt.base.Constants
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * 모든 소켓 매니저의 기본 클래스
 * - 연결, 재연결, 에러 처리, 이벤트 등록/해제
 */
abstract class BaseSocketManager(
    private val namespace: String
) {
    protected var socket: Socket? = null
    private var isManualDisconnect = false


    private val retryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val recentEmits = ConcurrentHashMap<String, EmitRecord>()

    private var lastErrorTime = 0L

    companion object {
        const val EVENT_RECONNECT = "reconnect"
        const val EVENT_ERROR = "error"

        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 1500L
        const val EMIT_TRACKING_DURATION_MS = 5000L
    }

    data class EmitRecord(
        val event: String,
        val data: JSONObject,
        val timestamp: Long,
        var retryCount: Int = 0
    )

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

                    if (code == 500) {
                        lastErrorTime = System.currentTimeMillis()
                        retryRecentEmits()
                    } else {
                        onError("$message (code: $code)")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[$namespace] 에러 파싱 실패")
                }
            }
        }
    }

    /**
     * 최근 전송한 이벤트들 재시도
     */
    private fun retryRecentEmits() {
        val now = System.currentTimeMillis()

        // 5초 이내에 전송한 이벤트만 재시도
        val recentEvents = recentEmits.values.filter {
            now - it.timestamp < EMIT_TRACKING_DURATION_MS
        }

        if (recentEvents.isEmpty()) {
            Timber.w("[$namespace] ⚠️ 재시도할 최근 이벤트 없음")
            return
        }

        Timber.d("[$namespace] 🔄 최근 ${recentEvents.size}개 이벤트 재시도 예약")

        recentEvents.forEach { record ->
            if (record.retryCount < MAX_RETRY_ATTEMPTS) {
                retryScope.launch {
                    delay(RETRY_DELAY_MS * (record.retryCount + 1)) // 점진적 지연

                    if (isConnected()) {
                        record.retryCount++
                        Timber.d("[$namespace] 🔄 재시도 ${record.retryCount}/$MAX_RETRY_ATTEMPTS: ${record.event}")
                        socket?.emit(record.event, record.data)
                    } else {
                        Timber.e("[$namespace] ❌ 재시도 실패: 소켓 연결 끊김")
                    }
                }
            } else {
                Timber.e("[$namespace] ❌ 최대 재시도 횟수 초과: ${record.event}")
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
        if (isRetryableEvent(event)) {
            val record = EmitRecord(
                event = event,
                data = data,
                timestamp = System.currentTimeMillis()
            )
            recentEmits[event] = record

            // 오래된 기록 정리
            cleanupOldEmits()
        }
    }


    /**
     * 재시도 가능한 이벤트인지 확인
     */
    private fun isRetryableEvent(event: String): Boolean {
        return when {
            event.contains("post now room info") -> true
            event.contains("post now ready info") -> true
            event.contains("post join room") -> true
            event.contains("post game info sync") -> true
            else -> false
        }
    }

    /**
     * 오래된 emit 기록 정리
     */
    private fun cleanupOldEmits() {
        val now = System.currentTimeMillis()
        recentEmits.entries.removeIf {
            now - it.value.timestamp > EMIT_TRACKING_DURATION_MS
        }
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
     * 이벤트 리스너 제거
     */
    protected fun off(event: String) {
        socket?.off(event)
//        Timber.d("[$namespace] 이벤트 리스너 제거: $event")
    }
}