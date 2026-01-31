package com.d104.pnt.util.socket

import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.domain.model.RoomInfoResponse
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 네임스페이스 소켓 매니저
 * 게임 로비(대기방) 관련 이벤트 처리
 */
@Singleton
class RoomSocketManager @Inject constructor(private val gson: Gson) : BaseSocketManager("room") {

    var currentRoomId: Long? = null

    // 의도적으로 나가는 중인지 확인하는 플래그 (기본값 false)
    var isIntentionalLeave: Boolean = false

    companion object {
        // Request Events (req)
        private const val EVENT_POST_JOIN_ROOM = "post join room"
        private const val EVENT_POST_UPDATE_ROOM_INFO = "post update room info"
        private const val EVENT_POST_UPDATE_READY = "post update ready"
        private const val EVENT_POST_NOW_READY_INFO = "post now ready info"
        private const val EVENT_POST_UPDATE_POSITION = "post update position"
        private const val EVENT_POST_NOW_ROOM_INFO = "post now room info"
        private const val EVENT_POST_MEMBER_KICK = "post member kick"
        private const val EVENT_POST_DISCONNECT = "post disconnect"

        // Response Events (res)
        private const val EVENT_GET_UPDATE_ROOM_INFO = "get update room info"
        private const val EVENT_GET_UPDATE_READY = "get update ready"
        private const val EVENT_GET_UPDATE_PREFER_POSITION = "get update prefer position"
        private const val EVENT_GET_NOW_READY_INFO = "get now ready info"
        private const val EVENT_GET_NOW_ROOM_INFO = "get now room info"
        private const val EVENT_GET_MEMBER_KICK = "get member kick"
        private const val EVENT_GET_DISCONNECT = "get disconnect"

        // Reconnect
        private const val EVENT_ROOM_RECONNECT = "reconnect"
    }

    // Callbacks
    private var onRoomInfoUpdated: ((JSONObject) -> Unit)? = null
    private var onReadyUpdated: ((Long, Long, Boolean) -> Unit)? = null
    private var onPositionUpdated: ((Long, Long, String) -> Unit)? = null
    private var onReadyInfoReceived: ((JSONArray) -> Unit)? = null
    private var onFullRoomInfoReceived: ((RoomInfoResponse) -> Unit)? = null
    private var onMemberKicked: ((Long) -> Unit)? = null
    private var onMemberLeft: ((Long) -> Unit)? = null
    private var onReconnected: ((Long) -> Unit)? = null

    override fun setupCustomListeners() {

        on(EVENT_ROOM_RECONNECT) { args ->
            try {
                val root = args[0] as JSONObject
                val roomId = root.optLong("roomId", 0L)
                if (roomId != 0L) {
                    Timber.d("🌐 [Socket] 1분 내 재연결 성공: roomId=$roomId")
                    currentRoomId = roomId
                    onReconnected?.invoke(roomId)
                }
            } catch (e: Exception) {
                Timber.e(e, "재연결 응답 파싱 실패")
            }
        }

        // Room info 업데이트 수신
        on(EVENT_GET_UPDATE_ROOM_INFO) { args ->
            try {
                val data = args[0] as JSONObject
                Timber.d("방 설정 업데이트: $data")
                onRoomInfoUpdated?.invoke(data)
            } catch (e: Exception) {
                Timber.e(e, "방 설정 업데이트 파싱 실패")
            }
        }

        // Ready 상태 업데이트 수신
        on(EVENT_GET_UPDATE_READY) { args ->
            try {
                val root = args[0] as JSONObject
                // 💡 'data' 객체가 있는지 확인하고 있으면 그 안에서 추출!
                val data = root.optJSONObject("data") ?: root

                val memberId = data.optLong("memberId", 0L)
                // 서버 키값이 'ready'인지 'isReady'인지 확인해서 둘 다 대응
                val isReady = when {
                    data.has("ready") -> data.getBoolean("ready")
                    data.has("isReady") -> data.getBoolean("isReady")
                    else -> false
                }

                if (memberId != 0L) {
                    Timber.d("✅ [Socket] Ready 파싱 성공: memberId=$memberId, ready=$isReady")
                    onReadyUpdated?.invoke(currentRoomId ?: 0L, memberId, isReady)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Ready 상태 업데이트 파싱 실패: ${e.message}")
            }
        }

        // 선호 포지션 업데이트 수신
        on(EVENT_GET_UPDATE_PREFER_POSITION) { args ->
            try {
                val root = args[0] as JSONObject
                Timber.d("📥 포지션 업데이트 수신 데이터: $root")

                val data = root.optJSONObject("data") ?: root

                val roomId = data.optLong("roomId", currentRoomId ?: 0L)
                val memberId = data.optLong("memberId", 0L)

                val preferPosition = data.optString("preferPosition", "THIEF")

                if (memberId != 0L) {
                    Timber.d("✅ 포지션 파싱 성공: memberId=$memberId, position=$preferPosition")
                    onPositionUpdated?.invoke(roomId, memberId, preferPosition)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 선호 포지션 업데이트 파싱 실패")
            }
        }

        // Ready 정보 리스트 수신
        on(EVENT_GET_NOW_READY_INFO) { args ->
            try {
                val data = args[0] as JSONArray
                Timber.d("Ready 정보 리스트: $data")
                onReadyInfoReceived?.invoke(data)
            } catch (e: Exception) {
                Timber.e(e, "Ready 정보 리스트 파싱 실패")
            }
        }

        // 전체 방 정보 수신
        on(EVENT_GET_NOW_ROOM_INFO) { args ->
            try {
                val jsonString = args[0].toString()
                val roomInfo = gson.fromJson(jsonString, RoomInfoResponse::class.java)

                Timber.d("전체 방 정보 수신 완료: ${roomInfo}")
                onFullRoomInfoReceived?.invoke(roomInfo)
            } catch (e: Exception) {
                Timber.e(e, "전체 방 정보 파싱 실패")
            }
        }

        // 멤버 강퇴 알림
        on(EVENT_GET_MEMBER_KICK) { args ->
            try {
                val root = args[0] as JSONObject
                Timber.d("📥 [Socket] 강퇴 이벤트 수신: $root")

                // 1. data가 null인 경우를 대비해 루트에서 직접 찾거나 data 안에서 찾음
                val data = root.optJSONObject("data") ?: root

                // 2. 서버가 "targetMemberId"로 주는지 "memberId"로 주는지 로그를 확인하여 둘 다 대응
                val kickedId = when {
                    data.has("memberId") -> data.getLong("memberId")
                    data.has("targetMemberId") -> data.getLong("targetMemberId")
                    root.has("memberId") -> root.getLong("memberId") // 루트에 있을 경우
                    else -> 0L
                }

                if (kickedId != 0L) {
                    Timber.d("✅ 강퇴 플레이어 식별 성공: $kickedId")
                    onMemberKicked?.invoke(kickedId)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 강퇴 파싱 실패")
            }
        }

        // 멤버 퇴장 알림
        on(EVENT_GET_DISCONNECT) { args ->
            try {
                val root = args[0] as JSONObject
                val data = root.optJSONObject("data") ?: root

                // memberId가 없을 수도 있으니 optLong으로 안전하게 추출
                val leftMemberId = data.optLong("memberId", 0L)

                if (leftMemberId != 0L) {
                    onMemberLeft?.invoke(leftMemberId)
                } else {
                    Timber.w("📥 [Socket] memberId 없는 퇴장 이벤트 수신 (무시)")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 멤버 퇴장 파싱 실패")
            }
        }
    }

    override fun onReconnect(data: JSONObject) {
        super.onReconnect(data)
        val roomId = data.optLong("roomId")
        if (roomId > 0) {
            currentRoomId = roomId
            // 재연결 시 방 정보 재요청
            requestRoomInfo(roomId)
        }
    }

    override fun onDisconnectCleanup() {
        if (isIntentionalLeave) {
            currentRoomId = null
        }

        // 이전에 정의한 콜백 초기화 함수 호출
        clearCallbacks()

        Timber.d("🧹 RoomSocketManager 전용 청소 완료 (의도적 퇴장 여부: $isIntentionalLeave)")
    }
    // ==================== Request Methods ====================

    /**
     * 방 입장
     */
    fun joinRoom(roomId: Long, onResponse: (Boolean, String) -> Unit) {
        if (!isConnected()) {
            Timber.e("소켓이 연결되지 않아 방 입장 불가")
            onResponse(false, "소켓 연결 안됨")
            return
        }

        currentRoomId = roomId

        // 방 입장 요청
        val joinData = JSONObject().apply {
            put("roomId", roomId)
        }

        emit(EVENT_POST_JOIN_ROOM, joinData)

        // 방 정보 요청
        requestRoomInfo(roomId)

        //  정보도 요청
        requestReadyInfo(roomId)

        onResponse(true, "입장 요청 완료")
    }

    /**
     * 현재 방 정보 요청
     */
    fun requestRoomInfo(roomId: Long = currentRoomId ?: 0) {
        if (roomId == 0L) {
            Timber.e("roomId가 없어서 방 정보 요청 불가")
            return
        }

        val data = JSONObject().apply {
            put("roomId", roomId)
        }

        emit(EVENT_POST_NOW_ROOM_INFO, data)
    }

    /**
     * 현재 Ready 정보 요청
     */
    fun requestReadyInfo(roomId: Long = currentRoomId ?: 0) {
        if (roomId == 0L) {
            Timber.e("roomId가 없어서 Ready 정보 요청 불가")
            return
        }

        val data = JSONObject().apply {
            put("roomId", roomId)
        }

        emit(EVENT_POST_NOW_READY_INFO, data)
    }

    /**
     * 방 설정 업데이트 (호스트만 가능)
     */
    fun updateRoomInfo(
        playerCount: Int,
        timeLimit: Int,
        policeCount: Int,
        thiefCount: Int,
        cctvInterval: Int,
        missionCount: Int,
        prison: Location?,
        polygon: List<Location>?
    ) {
        val roomId = currentRoomId ?: return
        val data = JSONObject().apply {
            put("roomId", roomId) // 방 ID 포함 여부는 서버 관례에 따라 확인
            put("playerCount", playerCount)
            put("timeLimit", timeLimit * 60) // 초 단위 변환
            put("policeCount", policeCount)
            put("thiefCount", thiefCount)
            put("cctvInterval", cctvInterval)
            put("missionCount", missionCount)

            // 감옥 좌표
            put("prison", JSONObject().apply {
                put("lat", prison?.lat)
                put("lng", prison?.lng)
            })

            // 폴리곤 좌표 배열
            val polyArray = JSONArray()
            polygon?.forEach { loc ->
                polyArray.put(JSONObject().apply {
                    put("lat", loc.lat)
                    put("lng", loc.lng)
                })
            }
            put("polygon", polyArray)
        }

        Timber.d("📤 [Socket] 방 설정 업데이트 전송: $data")
        emit(EVENT_POST_UPDATE_ROOM_INFO, data)
    }

    /**
     * Ready 상태 업데이트
     */
    fun updateReady(roomId: Long, ready: Boolean) {
        val currentRoomId = roomId

        val data = JSONObject().apply {
            put("roomId", roomId)
            put("ready", ready)
        }

        emit(EVENT_POST_UPDATE_READY, data)
    }

    /**
     * 선호 포지션 업데이트
     */
    fun updatePosition(preferPosition: String) {
        val roomId = currentRoomId ?: run {
            Timber.e("roomId가 없어서 포지션 업데이트 불가")
            return
        }


        val data = JSONObject().apply {
            put("roomId", roomId)
            put("preferPosition", preferPosition)
        }

        emit(EVENT_POST_UPDATE_POSITION, data)
    }


    /**
     * 멤버 강퇴 (호스트만 가능)
     */
    fun kickMember(targetMemberId: Long, reason: String, onSuccess: (Long) -> Unit) {
        val roomId = currentRoomId ?: return
        val data = JSONObject().apply {
            put("roomId", roomId)
            put("targetMemberId", targetMemberId)
            put("reason", reason)
        }

        emit(EVENT_POST_MEMBER_KICK, data)

        onSuccess(targetMemberId)
    }

    /**
     * 방 나가기 (정상 연결 해제)
     */
    fun leaveRoom() {
        val roomId = currentRoomId ?: return

        isIntentionalLeave = true

        val data = JSONObject().apply { put("roomId", roomId) }
        emit(EVENT_POST_DISCONNECT, data)

        disconnect()
    }

    // ==================== Callback Setters ====================

    fun setOnRoomInfoUpdated(callback: (JSONObject) -> Unit) {
        onRoomInfoUpdated = callback
    }

    fun setOnReadyUpdated(callback: (roomId: Long, memberId: Long, isReady: Boolean) -> Unit) {
        onReadyUpdated = callback
    }

    fun setOnPositionUpdated(callback: (roomId: Long, memberId: Long, position: String) -> Unit) {
        onPositionUpdated = callback
    }

    fun setOnReadyInfoReceived(callback: (JSONArray) -> Unit) {
        onReadyInfoReceived = callback
    }

    fun setOnFullRoomInfoReceived(callback: (RoomInfoResponse) -> Unit) {
        onFullRoomInfoReceived = callback
    }

    fun setOnMemberKicked(callback: (memberId: Long) -> Unit) {
        onMemberKicked = callback
    }

    fun setOnMemberLeft(callback: (memberId: Long) -> Unit) {
        onMemberLeft = callback
    }

    private fun clearCallbacks() {
        onRoomInfoUpdated = null
        onReadyUpdated = null
        onPositionUpdated = null
        onReadyInfoReceived = null
        onFullRoomInfoReceived = null
        onMemberKicked = null
        onMemberLeft = null
        onReconnected = null
    }

    override fun removeAllListeners() {
        super.removeAllListeners()
        off(EVENT_GET_UPDATE_ROOM_INFO)
        off(EVENT_GET_UPDATE_READY)
        off(EVENT_GET_UPDATE_PREFER_POSITION)
        off(EVENT_GET_NOW_READY_INFO)
        off(EVENT_GET_NOW_ROOM_INFO)
        off(EVENT_GET_MEMBER_KICK)
        off(EVENT_GET_DISCONNECT)
    }
}
