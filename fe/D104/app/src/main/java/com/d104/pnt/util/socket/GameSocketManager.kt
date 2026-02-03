package com.d104.pnt.util.socket

import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Game 네임스페이스 소켓 매니저
 * 실제 게임 플레이 중 이벤트 처리
 */
@Singleton
class GameSocketManager @Inject constructor() : BaseSocketManager("game") {

    var currentGameId: Long? = null

    companion object {
        // Request Events (req)
        private const val EVENT_POST_JOIN_ROOM = "post join room"
        private const val EVENT_POST_GPS = "post gps"
        private const val EVENT_POST_ARREST = "post arrest"
        private const val EVENT_POST_SKILL_USE = "post skill use"
        private const val EVENT_POST_MISSION_IMAGE = "post mission image"
        private const val EVENT_POST_SYNC_GAME_INFO = "post sync game info"
        private const val EVENT_POST_RESET_GAME = "post reset game"
        private const val EVENT_POST_AFTER_GAME_END = "post after game end"
        private const val EVENT_POST_DISCONNECT = "post disconnect"

        // Response Events (res)
        private const val EVENT_GET_JOIN_ROOM = "get join room"
        private const val EVENT_GET_GPS = "get gps"
        private const val EVENT_GET_WILL_START_GAME = "get will start game"
        private const val EVENT_GET_START_GAME = "get start game"
        private const val EVENT_GET_ESCAPE = "get escape"
        private const val EVENT_OUT_OF_BOUNDARY = "out of boundary"
        private const val EVENT_GET_ARREST = "get arrest"
        private const val EVENT_MODIFY_MEMBER_STATUS = "modify member status"
        private const val EVENT_GET_BEEP_USE = "get beep use"
        private const val EVENT_GET_SYNC_GAME_INFO = "get sync game info"
        private const val EVENT_GET_SKILL_USE = "get skill use"
        private const val EVENT_GET_END_GAME = "get end game"
        private const val EVENT_GET_END_GAME_AFTER = "get end game after"
        private const val EVENT_GET_NEWS = "get news"
        private const val EVENT_GET_RECONNECT = "reconnect"
    }

    // Callbacks
    private var onJoinedRoom: ((Long, Long, String) -> Unit)? = null
    private var onGpsReceived: ((Long?, String? ,Int, JSONArray) -> Unit)? = null
    private var onWillStartGame: ((Long, String) -> Unit)? = null
    private var onGameStarted: ((Long, String) -> Unit)? = null
    private var onThiefEscaped: ((gameId: Long, thiefId: Long, escapedAt: String) -> Unit)? = null
    private var onOutOfBoundary: ((Long, Long) -> Unit)? = null
    private var onArrestResult: ((String, String?, Long, Long, String?) -> Unit)? = null
    private var onMemberStatusChanged: ((Long, Long, String, String) -> Unit)? = null
    private var onBeepReceived: ((Long, Long, Double) -> Unit)? = null
    private var onGameInfoSynced: ((JSONObject) -> Unit)? = null
    private var onSkillResult: ((String, String?, Long, String?) -> Unit)? = null
    private var onEndGameAfter: ((JSONObject) -> Unit)? = null
    private var onGameEnded: ((String, String) -> Unit)? = null
    private var onNewsReceived: ((Long, Long) -> Unit)? = null
    private var onReconnected: ((Long) -> Unit)? = null

    private var onBeepUse: ((org.json.JSONObject) -> Unit)? = null


    override fun setupCustomListeners() {
        // 게임 입장 확인
        on(EVENT_GET_JOIN_ROOM) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getLong("gameId")
                val memberId = data.getLong("memberId")
                val message = data.getString("message")
                Timber.d("🎮 게임 입장 성공: gameId=$gameId, memberId=$memberId")
                onJoinedRoom?.invoke(gameId, memberId, message)
            } catch (e: Exception) {
                Timber.e(e, "게임 입장 응답 파싱 실패")
            }
        }

        // GPS 위치 정보 수신 (1초마다)
        on(EVENT_GET_GPS) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getInt("gameId")
                val cctvThiefId: Long? = if (data.isNull("cctvThiefId")) null else data.getLong("cctvThiefId")
                val skillUsedAt: String? = if (data.isNull("skillUsedAt")) null else (data.getString("skillUsedAt"))
                val sec = data.getInt("sec")
                val locations = data.getJSONArray("locations")
                Timber.d("GPS 위치 정보: gameId=$gameId, sec=$sec, 참여자=${locations.length()}명")
                onGpsReceived?.invoke(cctvThiefId, skillUsedAt, sec, locations)
            } catch (e: Exception) {
                Timber.e(e, "GPS 정보 파싱 실패")
            }
        }

        // 게임 시작 예정 알림 (5초 카운트다운)
        on(EVENT_GET_WILL_START_GAME) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getLong("gameId")
                val willStartAt = data.getString("willStartAt")
                Timber.d("게임 5초 후 시작: gameId=$gameId, willStartAt=$willStartAt")
                onWillStartGame?.invoke(gameId, willStartAt)
            } catch (e: Exception) {
                Timber.e(e, "게임 시작 예정 파싱 실패")
            }
        }

        // 게임 시작
        on(EVENT_GET_START_GAME) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getLong("gameId")
                val startTime = data.getString("startTime")
                Timber.d("게임 시작: gameId=$gameId, startTime=$startTime")
                onGameStarted?.invoke(gameId, startTime)
            } catch (e: Exception) {
                Timber.e(e, "게임 시작 파싱 실패")
            }
        }

        // 도둑 탈출
        on(EVENT_GET_ESCAPE) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getLong("gameId")
                val thiefId = data.getLong("thiefId")
                val escapedAt = data.getString("escapedAt")
                Timber.d("🏃 도둑 탈출 알림: thiefId=$thiefId,  escapedAt=$escapedAt")
                onThiefEscaped?.invoke(gameId, thiefId, escapedAt)
            } catch (e: Exception) {
                Timber.e(e, "도둑 탈출 파싱 실패")
            }
        }

        // 경계 이탈
        on(EVENT_OUT_OF_BOUNDARY) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getLong("gameId")
                val memberId = data.getLong("memberId")
                Timber.d("경계 이탈: gameId=$gameId, memberId=$memberId")
                onOutOfBoundary?.invoke(gameId, memberId)
            } catch (e: Exception) {
                Timber.e(e, "경계 이탈 파싱 실패")
            }
        }

        // 체포 결과
        on(EVENT_GET_ARREST) { args ->
            try {
                val data = args[0] as JSONObject
                val result = data.getString("result")
                val reason = data.optString("reason", null)
                val policeId = data.optLong("policeId", -1L)
                val thiefId = data.getLong("thiefId")
                val arrestedAt = data.optString("arrestedAt", null)
                Timber.d("체포 결과: result=$result, reason=$reason")
                onArrestResult?.invoke(result, reason, policeId, thiefId, arrestedAt)
            } catch (e: Exception) {
                Timber.e(e, "체포 결과 파싱 실패")
            }
        }

        // 멤버 상태 변경 (교도소 이송 등)
        on(EVENT_MODIFY_MEMBER_STATUS) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getLong("gameId")
                val thiefId = data.getLong("thiefId")
                val status = data.getString("status")
                val arrestedAt = data.getString("arrestedAt")
                Timber.d("멤버 상태 변경: thiefId=$thiefId, status=$status")
                onMemberStatusChanged?.invoke(gameId, thiefId, status, arrestedAt)
            } catch (e: Exception) {
                Timber.e(e, "멤버 상태 변경 파싱 실패")
            }
        }

        // Beep 알림 (경찰 접근)
        on(EVENT_GET_BEEP_USE) { args ->
            try {
                val data = args[0] as JSONObject
                val policeId = data.getLong("policeId")
                val thiefId = data.getLong("thiefId")
                val distance = data.getDouble("distance")
                Timber.d("Beep 알림: policeId=$policeId, distance=$distance")
                onBeepReceived?.invoke(policeId, thiefId, distance)
            } catch (e: Exception) {
                Timber.e(e, "Beep 알림 파싱 실패")
            }
        }

        // 게임 정보 동기화
        on(EVENT_GET_SYNC_GAME_INFO) { args ->
            try {
                val data = args[0] as JSONObject
                Timber.d("게임 정보 동기화: ${data.optString("gameStatus")}")
                onGameInfoSynced?.invoke(data)
            } catch (e: Exception) {
                Timber.e(e, "게임 정보 동기화 파싱 실패")
            }
        }

        // 스킬 사용 결과 (헬기)
        on(EVENT_GET_SKILL_USE) { args ->
            try {
                val data = args[0] as JSONObject
                val result = data.getString("result")
                val reason = data.optString("reason", null)
                val policeId = data.getLong("policeId")
                val startedAt = data.optString("startedAt", null)
                Timber.d("스킬 사용 결과: result=$result, policeId=$policeId")
                onSkillResult?.invoke(result, reason, policeId, startedAt)
            } catch (e: Exception) {
                Timber.e(e, "스킬 사용 결과 파싱 실패")
            }
        }

        // 게임 종료
        on(EVENT_GET_END_GAME) { args ->
            try {
                val data = args[0] as JSONObject

                val winTeam = data.getString("winTeam")

                val message = data.optString("message", "게임이 종료되었습니다.")

                Timber.d("🎮 게임 종료 수신: 승리팀=$winTeam, 메시지=$message")
                onGameEnded?.invoke(winTeam, message)
            } catch (e: Exception) {
                Timber.e(e, "❌ 게임 종료 파싱 실패: ${e.message}")
                Timber.e("받은 데이터: ${args[0]}")
            }
        }

        // 게임 종료 후 상세 결과 수신
        on(EVENT_GET_END_GAME_AFTER) { args ->
            try {
                val data = args[0] as JSONObject
                Timber.d("$data")
                onEndGameAfter?.invoke(data)
            } catch (e: Exception) {
                Timber.e(e, "게임 결과 파싱 실패")
            }
        }

        on(EVENT_GET_NEWS) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.getLong("gameId")
                val newsId = data.getLong("newsId")

                Timber.d("$data")
                Timber.d("📰 뉴스 생성 완료: gameId=$gameId, newsId=$newsId")
                onNewsReceived?.invoke(gameId, newsId)
            } catch (e: Exception) {
                Timber.e(e, "게임 종료 파싱 실패")
            }
        }

        on(EVENT_GET_RECONNECT) { args ->
            try {
                val data = args[0] as JSONObject
                val gameId = data.optLong("gameId")
                Timber.d("🔄 서버로부터 재연결 승인 수신: gameId=$gameId")

                // 기존에 정의하신 onReconnect 호출 (내부에서 syncGameInfo() 실행)
                onReconnect(data)

                // ViewModel 등에 알림
                onReconnected?.invoke(gameId)
            } catch (e: Exception) {
                Timber.e(e, "재연결 데이터 파싱 실패")
            }
        }
    }


    override fun onReconnect(data: JSONObject) {
        super.onReconnect(data)
        val gameId = data.optLong("gameId")
        if (gameId > 0) {
            currentGameId = gameId
            // 재연결 시 게임 정보 동기화
            syncGameInfo()
        }
    }

    override fun onDisconnectCleanup() {
        currentGameId = null
        clearCallbacks()
    }

    // ==================== Request Methods ====================

    /**
     * 게임 입장
     */
    fun joinGame(gameId: Long) {
        currentGameId = gameId

        val data = JSONObject().apply {
            put("gameId", gameId)
        }

        emit(EVENT_POST_JOIN_ROOM, data)
    }

    /**
     * GPS 위치 전송
     */
    fun sendGPS(lat: Double, lng: Double, walk: Int, longestSurvived: Int) {
        val gameId = currentGameId ?: run {
            Timber.e("gameId가 없어서 GPS 전송 불가")
            return
        }

        val data = JSONObject().apply {
            put("gameId", gameId)
            put("lat", lat)
            put("lng", lng)
            put("walk", walk)
            put("longestSurvived", longestSurvived)
        }

        emit(EVENT_POST_GPS, data)
    }

    /**
     * 체포 시도
     */
    fun arrestThief(policeId: Long, thiefId: Long) {
        val gameId = currentGameId ?: run {
            Timber.e("gameId가 없어서 체포 불가")
            return
        }

        val data = JSONObject().apply {
            put("gameId", gameId)
            put("policeId", policeId)
            put("thiefId", thiefId)
        }

        emit(EVENT_POST_ARREST, data)
    }

    /**
     * 스킬 사용 (헬기)
     */
    fun useSkill(policeId: Long) {
        val gameId = currentGameId ?: run {
            Timber.e("gameId가 없어서 스킬 사용 불가")
            return
        }

        val data = JSONObject().apply {
            put("gameId", gameId)
            put("policeId", policeId)
        }

        emit(EVENT_POST_SKILL_USE, data)
    }

    /**
     * 미션 이미지 제출
     */
    fun submitMissionImage(memberId: Long, gameMissionId: Long, image: String) {
        val gameId = currentGameId ?: run {
            Timber.e("gameId가 없어서 미션 이미지 제출 불가")
            return
        }

        val data = JSONObject().apply {
            put("gameId", gameId)
            put("memberId", memberId)
            put("gameMissionId", gameMissionId)
            put("image", image)
        }

        emit(EVENT_POST_MISSION_IMAGE, data)
    }

    /**
     * 게임 정보 동기화 요청
     */
    fun syncGameInfo() {
        val gameId = currentGameId ?: run {
            Timber.e("gameId가 없어서 게임 정보 동기화 불가")
            return
        }

        val data = JSONObject().apply {
            put("gameId", gameId)
        }

        emit(EVENT_POST_SYNC_GAME_INFO, data)
    }

    /**
     * 게임 리셋 (개발용 - 사용 금지)
     */
    @Deprecated("개발용이므로 프로덕션에서 사용 금지")
    fun resetGame() {
        val gameId = currentGameId ?: run {
            Timber.e("gameId가 없어서 게임 리셋 불가")
            return
        }

        val data = JSONObject().apply {
            put("gameId", gameId)
        }

        Timber.w("게임 리셋 요청 (개발용)")
        emit(EVENT_POST_RESET_GAME, data)
    }

    /**
     * 게임 나가기
     */
    fun leaveGame() {
        val gameId = currentGameId ?: run {
            Timber.e("gameId가 없어서 게임 나가기 불가")
            return
        }

        val data = JSONObject().apply {
            put("gameId", gameId)
        }

        emit(EVENT_POST_DISCONNECT, data)

        // 연결 해제
        disconnect()
    }

    /**
     * 게임 종료 후 상세 결과 요청
     */
    fun postAfterGameEnd(gameId: Long) {
        val data = JSONObject().apply {
            put("gameId", gameId)
        }
        emit(EVENT_POST_AFTER_GAME_END, data)
    }

    fun setOnReconnected(callback: (gameId: Long) -> Unit) {
        onReconnected = callback
    }


    // ==================== Callback Setters ====================

    fun setOnJoinedRoom(callback: (gameId: Long, memberId: Long, message: String) -> Unit) {
        onJoinedRoom = callback
    }

    fun setOnGpsReceived(callback: (cctvThiefId: Long?, skillUsedAt: String?, sec: Int, locations: JSONArray) -> Unit) {
        onGpsReceived = callback
    }

    fun setOnWillStartGame(callback: (gameId: Long, willStartAt: String) -> Unit) {
        onWillStartGame = callback
    }

    fun setOnGameStarted(callback: (gameId: Long, startTime: String) -> Unit) {
        onGameStarted = callback
    }

    fun setOnThiefEscaped(callback: (gameId: Long, thiefId: Long, escapedAt: String) -> Unit) {
        onThiefEscaped = callback
    }

    fun setOnOutOfBoundary(callback: (gameId: Long, memberId: Long) -> Unit) {
        onOutOfBoundary = callback
    }

    fun setOnArrestResult(callback: (result: String, reason: String?, policeId: Long, thiefId: Long, arrestedAt: String?) -> Unit) {
        onArrestResult = callback
    }

    fun setOnMemberStatusChanged(callback: (gameId: Long, thiefId: Long, status: String, arrestedAt: String) -> Unit) {
        onMemberStatusChanged = callback
    }

    fun setOnBeepReceived(callback: (policeId: Long, thiefId: Long, distance: Double) -> Unit) {
        onBeepReceived = callback
    }

    fun setOnGameInfoSynced(callback: (JSONObject) -> Unit) {
        onGameInfoSynced = callback
    }

    fun setOnSkillResult(callback: (result: String, reason: String?, policeId: Long, startedAt: String?) -> Unit) {
        onSkillResult = callback
    }


    fun setOnEndGameAfter(callback: (JSONObject) -> Unit) {
        onEndGameAfter = callback
    }

    fun setOnGameEnded(callback: (winnerPosition: String, message: String) -> Unit) {
        onGameEnded = callback
    }

    fun setOnNewsReceived(callback: (gameId: Long, newsId: Long) -> Unit) {
        onNewsReceived = callback
    }

    fun cleanup() {
        Timber.d("🧹 Game 소켓 완전 정리")
        clearCallbacks()
        if (isConnected()) {
            disconnect()
        }
    }

    private fun clearCallbacks() {
        onJoinedRoom = null
        onGpsReceived = null
        onWillStartGame = null
        onGameStarted = null
        onThiefEscaped = null
        onOutOfBoundary = null
        onArrestResult = null
        onMemberStatusChanged = null
        onBeepReceived = null
        onGameInfoSynced = null
        onSkillResult = null
        onGameEnded = null
    }

    public override fun removeAllListeners() {
        super.removeAllListeners()
        off(EVENT_GET_JOIN_ROOM)
        off(EVENT_GET_GPS)
        off(EVENT_GET_WILL_START_GAME)
        off(EVENT_GET_START_GAME)
        off(EVENT_GET_ESCAPE)
        off(EVENT_OUT_OF_BOUNDARY)
        off(EVENT_GET_ARREST)
        off(EVENT_MODIFY_MEMBER_STATUS)
        off(EVENT_GET_BEEP_USE)
        off(EVENT_GET_SYNC_GAME_INFO)
        off(EVENT_GET_SKILL_USE)
        off(EVENT_GET_END_GAME)
    }
}