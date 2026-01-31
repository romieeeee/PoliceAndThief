package com.d104.pnt.util.socket

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
    }

    // Callbacks
    private var onRoomInfoUpdated: ((JSONObject) -> Unit)? = null
    private var onReadyUpdated: ((Long, Long, Boolean) -> Unit)? = null
    private var onPositionUpdated: ((Long, Long, String) -> Unit)? = null
    private var onReadyInfoReceived: ((JSONArray) -> Unit)? = null
    private var onFullRoomInfoReceived: ((RoomInfoResponse) -> Unit)? = null
    private var onMemberKicked: ((Long) -> Unit)? = null
    private var onMemberLeft: ((Long) -> Unit)? = null

    override fun setupCustomListeners() {
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
                Timber.d("📥 [Socket] get member kick 수신: $root")

                // 💡 서버 응답 구조에 따라 data 객체 혹은 루트에서 memberId 추출
                val data = root.optJSONObject("data") ?: root
                val kickedMemberId = data.optLong("memberId", 0L)

                if (kickedMemberId != 0L) {
                    Timber.d("✅ 강퇴 알림 파싱 성공: $kickedMemberId")
                    onMemberKicked?.invoke(kickedMemberId)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 멤버 강퇴 이벤트 파싱 실패")
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
        currentRoomId = null
        clearCallbacks()
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
    fun updateRoomSettings(
        playerCount: Int,
        timeLimit: Int,
        policeCount: Int,
        thiefCount: Int,
        cctvInterval: Int,
        prisonLat: Double,
        prisonLng: Double,
        polygon: List<Pair<Double, Double>>
    ) {
        val roomId = currentRoomId ?: run {
            Timber.e("roomId가 없어서 방 설정 업데이트 불가")
            return
        }

        val polygonArray = JSONArray().apply {
            polygon.forEach { (lat, lng) ->
                put(JSONObject().apply {
                    put("lat", lat)
                    put("lng", lng)
                })
            }
        }

        val data = JSONObject().apply {
            put("playerCount", playerCount)
            put("timeLimit", timeLimit)
            put("policeCount", policeCount)
            put("thiefCount", thiefCount)
            put("cctvInterval", cctvInterval)
            put("prison", JSONObject().apply {
                put("lat", prisonLat)
                put("lng", prisonLng)
            })
            put("polygon", polygonArray)
        }

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
    fun kickMember(targetMemberId: Long, reason: String) {
        val roomId = currentRoomId ?: run {
            Timber.e("roomId가 없어서 멤버 강퇴 불가")
            return
        }

        val data = JSONObject().apply {
            put("roomId", roomId)
            put("targetMemberId", targetMemberId)
            put("reason", reason)
        }

        Timber.d("📤 post member kick 전송 시도: roomId = $roomId, target = $targetMemberId")
        emit(EVENT_POST_MEMBER_KICK, data)
    }

    /**
     * 방 나가기 (정상 연결 해제)
     */
    fun leaveRoom() {
        val roomId = currentRoomId ?: run {
            Timber.e("roomId가 없어서 방 나가기 불가")
            return
        }

        val data = JSONObject().apply {
            put("roomId", roomId)
        }

        emit(EVENT_POST_DISCONNECT, data)

        // 연결 해제
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