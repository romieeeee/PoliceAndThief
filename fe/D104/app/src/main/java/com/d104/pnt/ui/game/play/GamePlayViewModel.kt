package com.d104.pnt.ui.game.play

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.base.Constants
import com.d104.pnt.data.remote.model.response.BeepUseResponse
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.data.repository.WalkieRepository
import com.d104.pnt.domain.model.PlayerData
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.service.game.GameActiveService
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.getSingleLocation
import com.d104.pnt.util.socket.GameSocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class GamePlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val gameSocketManager: GameSocketManager,
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val stepSensorManager: StepSensorManager,
    private val walkieRepository: WalkieRepository
) : ViewModel() {
    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L
    private val roleString: String = savedStateHandle.get<String>(NavArgs.ROLE) ?: "THIEF"

    private val _uiEvent = MutableSharedFlow<GameSessionEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEvent = _uiEvent.asSharedFlow()
    val isOutOfBoundary = gameSessionRepository.isOutOfBoundary

    // ===== Beep 이벤트 (도둑 쪽에서만 화면이 소리 재생하도록 Screen에서 필터) =====
    // Beep 이벤트
    private val _beepEvent = MutableSharedFlow<BeepUseResponse>(extraBufferCapacity = 16)
    val beepEvent = _beepEvent.asSharedFlow()

    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    // raw GPS -> repo 저장값
    val playerLocations = locationRepository.playerLocations

    private val _allMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    val allMembers: StateFlow<List<GameMemberSocketDto>> = _allMembers.asStateFlow()
    val members = gameSessionRepository.members

    val gameStatus = gameSessionRepository.gameStatus
    val missions = gameSessionRepository.missions

    val thiefMembers = _allMembers
        .map { list -> list.filter { it.position.equals("THIEF", ignoreCase = true) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isOutOfBoundary = MutableStateFlow(false)

    private val _escapeQueue = MutableStateFlow<List<String>>(emptyList())
    val escapeQueue = _escapeQueue.asStateFlow()

    private val _myMemberId = MutableStateFlow(0L)
    val myMemberId: StateFlow<Long> = _myMemberId.asStateFlow()

    private var warningJob: Job? = null
    private var pttHeartbeatJob: Job? = null
    private var radioTimeoutJob: Job? = null

    // ===== 무전기 =====
    val walkieConnected = walkieRepository.isConnected
    val walkieMicEnabled = walkieRepository.isMicEnabled
    val walkieParticipantCount = walkieRepository.participantCount

    private val _walkieState = MutableStateFlow<WalkieConnectionState>(WalkieConnectionState.Idle)
    val walkieState = _walkieState.asStateFlow()

    private val _isSomeoneTalking = MutableStateFlow(false)
    val isSomeoneTalking = _isSomeoneTalking.asStateFlow()


    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting = _isTransmitting.asStateFlow()

    private val _talkingMemberId = MutableStateFlow<Long?>(null)

    val talkingMemberId = _talkingMemberId.asStateFlow()


    // =========================
    // 🚁 Helicopter Skill State
    // =========================
    private val _helicopterState = MutableStateFlow(HelicopterUiState())
    val helicopterState = _helicopterState.asStateFlow()

    // "게임당 1회"
    private val _helicopterUsed = MutableStateFlow(false)
    val helicopterUsed: StateFlow<Boolean> = _helicopterUsed.asStateFlow()

    // CCTV로 잡힌 도둑(평소 1명 공개용)
    private val _cctvThiefId = MutableStateFlow<Long?>(null)
    val cctvThiefId = _cctvThiefId.asStateFlow()

    // 경찰 미니맵에서 표시할 플레이어들(경찰 + (CCTV 1명 도둑 or 도둑 전체))
    private val _minimapPlayers = MutableStateFlow<List<PlayerData>>(emptyList())
    val minimapPlayers: StateFlow<List<PlayerData>> = _minimapPlayers.asStateFlow()


    // ✅ 청장 memberId (null이면 0으로 치환해서 UI에서 쓰기 편하게)
    val chiefMemberId: StateFlow<Long> =
        gameSessionRepository.chiefMemberId
            .map { it ?: 0L }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /**
     * ✅ 청장 여부
     * - gameSessionRepository에 chiefMemberId가 있어야 함 (StateFlow<Long?> 같은 형태)
     */
    val isChief: StateFlow<Boolean> =
        combine(myMemberId, gameSessionRepository.chiefMemberId) { myId, chiefId ->
            chiefId != null && myId != 0L && myId == chiefId
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * ✅ 버튼 enable 여부
     */
    val helicopterButtonEnabled: StateFlow<Boolean> =
        combine(isChief, helicopterUsed) { chief, used ->
            chief && !used
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        fetchMyId()
        setupSocketListeners()
        gameSessionRepository.gameInit()
        observeRepositoryEvents()
        startService(GameActiveService.ACTION_START)
    }

    private fun observeRepositoryEvents() {
        viewModelScope.launch {
            gameSessionRepository.eventFlow.collect { event ->
                when (event) {
                    is GameSessionEvent.NavigateToLoading -> {
                        _uiEvent.emit(event)
                    }

                    is GameSessionEvent.NavigateToNews -> {
                        _uiEvent.emit(event)
                    }

                    is GameSessionEvent.GameStarted -> {
                        startService(GameActiveService.ACTION_START)
                    }

                    else -> {
                        Timber.d("기타 이벤트 처리: $event")
                    }
                }
            }
        }
    }

    private fun fetchMyId() {
        viewModelScope.launch {
            authRepository.getMemberId().collect { id ->
                if (id != 0L) _myMemberId.value = id
            }
        }
    }

    fun initGame() {
        viewModelScope.launch {
            val token = authRepository.getAccessToken().first()
            if (token.isNotEmpty() && !gameSocketManager.isConnected()) {
                gameSocketManager.connect(token)
                while (!gameSocketManager.isConnected()) {
                    delay(100)
                }
            }

            gameSocketManager.joinGame(gameId)
            delay(300)
            gameSocketManager.syncGameInfo()
        }
    }

    private fun setupSocketListeners() {
        // beep 수신
        gameSocketManager.setOnBeepReceived { policeId, thiefId, distance ->
            _beepEvent.tryEmit(BeepUseResponse(policeId, thiefId, distance))
            Timber.d("📢 beep 수신: policeId=$policeId, thiefId=$thiefId, distance=$distance")
        }

        // GPS 수신 (locations + cctvThiefId + skillUsedAt)
        gameSocketManager.setOnGpsReceived { _, locations, cctvThiefId, skillUsedAt ->
            val list = parsePlayersFromGps(locations)
            locationRepository.updatePlayerLocation(list)

            _cctvThiefId.value = cctvThiefId

            // ✅ skillUsedAt이 내려오면 "누가 썼든" 게임당 1회 사용된 상태
            if (!skillUsedAt.isNullOrBlank()) {
                _helicopterUsed.value = true
            }

            updateHelicopterStateFromSkillUsedAt(skillUsedAt)
            recomputeMinimapPlayers(raw = list)
        }

        // 게임 정보 동기화
        gameSocketManager.setOnGameInfoSynced { data ->
            viewModelScope.launch {
                try {
                    val membersArray = data.optJSONArray("members") ?: return@launch
                    val newMembers = mutableListOf<GameMemberSocketDto>()
                    for (i in 0 until membersArray.length()) {
                        val memberJson = membersArray.getJSONObject(i)
                        newMembers.add(GameMemberSocketDto.fromJson(memberJson))
                    }
                    updateMembersList(newMembers)
                    Timber.d("GamePlayViewModel: 전체 멤버 동기화 완료 (${newMembers.size}명)")
                } catch (e: Exception) {
                    Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
                }
            }
        }

        // 상태 변경
        gameSocketManager.setOnMemberStatusChanged { _, thiefId, status, _ ->
            viewModelScope.launch {
                val currentList = _allMembers.value.toMutableList()
                val idx = currentList.indexOfFirst { it.memberId == thiefId }
                if (idx != -1) {
                    val old = currentList[idx]
                    currentList[idx] = old.copy(rawStatus = status)
                    updateMembersList(currentList)
                    Timber.d("GamePlayViewModel: 도둑($thiefId) 상태 변경 -> $status")
                }
            }
        }

        // ✅ 스킬 결과(성공/실패) 수신: 실패면 used 롤백
        gameSocketManager.setOnSkillResult { result, reason, policeId, startedAt ->
            Timber.d("🚁 스킬 결과 수신: result=$result, reason=$reason, policeId=$policeId, startedAt=$startedAt")

            val success = result.equals("SUCCESS", ignoreCase = true)

            if (success) {
                _helicopterUsed.value = true // 확정
            } else {
                // 내가 눌렀던 건데 실패면 롤백해줘야 버튼이 다시 살아남
                val myId = _myMemberId.value
                if (policeId == myId) {
                    _helicopterUsed.value = false
                }
            }
        }

        // 도둑 탈출 수신
        gameSocketManager.setOnThiefEscaped { gameId, thiefId, escapedAt ->
            viewModelScope.launch {
                Timber.d("🏃 도둑 탈출 알림 수신: thiefId=$thiefId, escapedAt=$escapedAt")

                val escapedThief = _allMembers.value.find { it.memberId == thiefId }
                val thiefNickname = escapedThief?.nickname ?: "도둑"

                _escapeQueue.value = _escapeQueue.value + thiefNickname
                Timber.d("📋 탈출 큐에 추가: $thiefNickname (현재 큐 크기: ${_escapeQueue.value.size})")
            }
        }

        gameSocketManager.setOnRadioReceived { _, memberId ->
            handleRadioSignal(memberId)
        }

        // 게임 종료
        gameSocketManager.setOnGameEnded { winnerPosition, _ ->
            Timber.d("🏁 게임 종료 수신: $winnerPosition 승리")
            gameSocketManager.postAfterGameEnd(gameId)
        }

//        // 게임 종료 상세
//        gameSocketManager.setOnEndGameAfter {
//            viewModelScope.launch {
//                _uiEvent.emit(GameSessionEvent.NavigateToNews(gameId))
//                viewModelScope.launch { gameRepository.gameHardDelete(gameId) } // TODO: 개발용
//            }
//        }
    }

    fun removeFirstEscape() {
        _escapeQueue.value = _escapeQueue.value.drop(1)
        Timber.d("📋 탈출 큐에서 제거 (남은 큐 크기: ${_escapeQueue.value.size})")
    }

    private fun updateMembersList(newList: List<GameMemberSocketDto>) {
        _allMembers.value = newList
        Timber.d(
            "👥 멤버 리스트 갱신: ${newList.size}명 / thief=${
                newList.count {
                    it.position.equals(
                        "THIEF",
                        true
                    )
                }
            }"
        )
    }

    fun setDefaultArea(context: Context) {
        viewModelScope.launch {
            val location = context.getSingleLocation()
            if (location != null) {
                locationRepository.updateCurrentLocation(location)
                locationRepository.createDefaultPolygon(location)
                locationRepository.setPrisonLocation(LatLng(location.latitude, location.longitude))
            }
        }
    }

    // =========================
    // ✅ 헬기 스킬 사용(청장만, 게임당 1회)
    // =========================
    fun useHelicopterSkill() {
        val myId = _myMemberId.value
        val chiefId = gameSessionRepository.chiefMemberId.value

        // 청장만
        if (chiefId == null || myId == 0L || myId != chiefId) {
            Timber.w("🚁 스킬 사용 불가: 청장 아님 (my=$myId, chief=$chiefId)")
            return
        }

        // 1회 제한
        if (_helicopterUsed.value) {
            Timber.w("🚁 스킬 사용 불가: 이미 사용됨")
            return
        }

        // ✅ 중복 탭 방지: 누르는 순간 잠궈둠 (실패하면 onSkillResult에서 롤백)
        _helicopterUsed.value = true

        // ✅ 소켓 발행
        gameSocketManager.useSkill(policeId = myId)
        Timber.d("🚁 post skill use 요청: gameId=$gameId, policeId=$myId")
    }

    // =========================
    // 🚁 Helicopter State Logic
    // =========================
    private fun updateHelicopterStateFromSkillUsedAt(skillUsedAtRaw: String?) {
        if (skillUsedAtRaw.isNullOrBlank()) {
            _helicopterState.value = HelicopterUiState()
            return
        }

        val usedAt = try {
            Instant.parse(skillUsedAtRaw)
        } catch (e: Exception) {
            Timber.e(e, "🚁 skillUsedAt 파싱 실패: $skillUsedAtRaw")
            return
        }

        val now = Instant.now()
        val notifyEnd = usedAt.plusSeconds(5)
        val revealEnd = usedAt.plusSeconds(15)

        val phase = when {
            now.isBefore(notifyEnd) -> HelicopterPhase.NOTIFY
            now.isBefore(revealEnd) -> HelicopterPhase.REVEAL
            else -> HelicopterPhase.IDLE
        }

        val remainingMs = when (phase) {
            HelicopterPhase.NOTIFY ->
                (notifyEnd.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)

            HelicopterPhase.REVEAL ->
                (revealEnd.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)

            HelicopterPhase.IDLE -> 0L
        }

        _helicopterState.value = HelicopterUiState(
            usedAt = if (phase == HelicopterPhase.IDLE) null else usedAt,
            phase = phase,
            notifyEndsAt = if (phase != HelicopterPhase.IDLE) notifyEnd else null,
            revealEndsAt = if (phase != HelicopterPhase.IDLE) revealEnd else null,
            remainingMs = remainingMs
        )
    }

    private fun parsePlayersFromGps(locations: JSONArray): List<PlayerData> {
        val result = ArrayList<PlayerData>(locations.length())
        for (i in 0 until locations.length()) {
            val o = locations.optJSONObject(i) ?: continue

            val memberId = o.optLong("memberId", 0L)
            val gameIdRaw = o.opt("gameId")
            val parsedGameId = when (gameIdRaw) {
                is Number -> gameIdRaw.toLong()
                is String -> gameIdRaw.toLongOrNull() ?: 0L
                else -> 0L
            }

            val lat = o.optDouble("lat", 0.0)
            val lng = o.optDouble("lng", 0.0)
            val walk = o.optInt("walk", 0)
            val longestSurvived = o.optInt("longestSurvived", 0)
            val position = o.optString("position", "")
            val status = o.optString("status", "")
            val penalty = o.optInt("penalty", 0)
            val timeStamp = o.optString("timestamp", "")

            result.add(
                PlayerData(
                    id = 0L,
                    gameId = parsedGameId,
                    memberId = memberId,
                    lat = lat,
                    lng = lng,
                    walk = walk,
                    longestSurvived = longestSurvived,
                    position = position,
                    status = status,
                    penalty = penalty,
                    timeStamp = timeStamp
                )
            )
        }
        return result
    }

    private fun recomputeMinimapPlayers(raw: List<PlayerData>) {
        val myId = _myMemberId.value

        val police = raw.filter {
            it.position.equals("POLICE", ignoreCase = true) && it.memberId != myId
        }

        val thieves = raw.filter { it.position.equals("THIEF", ignoreCase = true) }

        val visibleThieves = when (helicopterState.value.phase) {
            HelicopterPhase.REVEAL -> thieves
            else -> {
                val targetId = _cctvThiefId.value
                if (targetId == null) emptyList()
                else thieves.filter { it.memberId == targetId }
            }
        }

        _minimapPlayers.value = police + visibleThieves
    }

    private fun startService(action: String) {
        Intent(context, GameActiveService::class.java).also { intent ->
            intent.action = action

            if (action == GameActiveService.ACTION_STOP) {
                context.startService(intent)
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)

                }
            }
        }
    }


    fun manualLeaveGame() {
        viewModelScope.launch {
            Timber.d("🚪 유저가 직접 게임 종료를 선택함")
            // 1. GPS 서비스 중단
            startService(GameActiveService.ACTION_STOP)
            // 2. 소켓 연결 해제 및 세션 정리 (post disconnect 포함)
            gameSessionRepository.leaveGame()
        }
    }

    /**
     * ====================
     * 무전기 연결 (경찰만)
     * ====================
     */
    fun connectWalkie() {
        // 경찰이 아니면 무시
        if (roleString != "POLICE") {
            Timber.d("🎙️ 도둑은 무전기 연결 안함")
            return
        }

        viewModelScope.launch {
            try {
                _walkieState.value = WalkieConnectionState.Connecting

                // 1. roomCode 가져오기
                val roomCode = gameSessionRepository.roomCode.first()
                if (roomCode.isBlank()) {
                    _walkieState.value = WalkieConnectionState.Error("방 코드가 없습니다")
                    Timber.e("🎙️ 무전기 연결 실패: roomCode 없음")
                    return@launch
                }

                Timber.d("🎙️ LiveKit 토큰 요청: roomCode=$roomCode")

                // 2. 토큰 발급
                when (val result = gameRepository.getLiveKitToken(roomCode)) {
                    is BaseResult.Success -> {
                        val tokenResponse = result.data
                        Timber.d("🎙️ 토큰 발급 성공: ${tokenResponse.identity}")

                        // 3. LiveKit 연결
                        walkieRepository.connect(
                            serverUrl = Constants.LIVEKIT_URL,
                            token = tokenResponse.token,
                            roomName = tokenResponse.roomCode
                        )

                        _walkieState.value = WalkieConnectionState.Connected
                        Timber.d("🎙️ 무전기 연결 완료")
                    }

                    is BaseResult.Error -> {
                        val errorMsg = result.error.message ?: "토큰 발급 실패"
                        _walkieState.value = WalkieConnectionState.Error(errorMsg)
                        Timber.e("🎙️ 토큰 발급 실패: $errorMsg")
                    }
                }
            } catch (e: Exception) {
                _walkieState.value = WalkieConnectionState.Error(e.message ?: "연결 실패")
                Timber.e(e, "🎙️ 무전기 연결 실패")
            }
        }
    }

    fun disconnectWalkie() {
        viewModelScope.launch {
            try {
                walkieRepository.disconnect()
                _walkieState.value = WalkieConnectionState.Idle
                Timber.d("🎙️ 무전기 연결 해제")
            } catch (e: Exception) {
                Timber.e(e, "🎙️ 연결 해제 실패")
            }
        }
    }

    private fun handleRadioSignal(memberId: Long) {
        if (_isTransmitting.value) {
            Timber.d("📻 Radio : [필터] 내가 송신 중이므로 수신 신호 무시")
            return
        }

        if (memberId == myMemberId.value || myMemberId.value == 0L) {
            return
        }

        viewModelScope.launch {
            // 3. 다른 사람이 말하고 있음을 표시
            _isSomeoneTalking.value = true
            _talkingMemberId.value = memberId

            Timber.d("📻 Radio: [수신] memberId=$memberId 송신 중")

            // 4. 타이머: 1.5초 동안 다음 신호가 안 오면 종료
            radioTimeoutJob?.cancel()
            radioTimeoutJob = launch {
                delay(1500) // heartbeat 1초 + 여유 0.5초
                _isSomeoneTalking.value = false
                _talkingMemberId.value = null
                Timber.d("📻 Radio: [수신] 무전 신호 종료 (Timeout)")
            }
        }


        viewModelScope.launch {
            // 2. 남이 말하고 있음을 표시
            _isSomeoneTalking.value = true
            _talkingMemberId.value = memberId

            // 3. 타이머 리셋: 1초 동안 다음 신호가 안 오면 종료로 간주
            radioTimeoutJob?.cancel()
            radioTimeoutJob = launch {
                delay(1000)
                _isSomeoneTalking.value = false
                _talkingMemberId.value = null
                Timber.d("📻 Radio: [수신] 무전 신호 종료 (Timeout)")
            }
        }
    }

    fun startTalking() {
        if (roleString != "POLICE") return

        viewModelScope.launch {
            try {
                launch { walkieRepository.enableMic() }

                pttHeartbeatJob?.cancel()
                pttHeartbeatJob = launch {
                    while (isActive) {
                        gameSocketManager.sendRadio()
                        Timber.d("📻 [PTT] Heartbeat 송신 중...")
                        delay(1000) // 서버 전송 주기
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "🎙️ PTT 시작 실패")
            }
        }
    }

    fun stopTalking() {
        if (roleString != "POLICE") return

        viewModelScope.launch {
            try {
                // 1. Heartbeat 중단
                pttHeartbeatJob?.cancel()
                pttHeartbeatJob = null

                // 2. LiveKit 마이크 비활성화
                walkieRepository.disableMic()

                Timber.d("🎙️ [PTT] 송신 중지")
            } catch (e: Exception) {
                Timber.e(e, "🎙️ PTT 중지 실패")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        disconnectWalkie()
        startService(GameActiveService.ACTION_STOP)
    }
}

sealed class WalkieConnectionState {
    object Idle : WalkieConnectionState()
    object Connecting : WalkieConnectionState()
    object Connected : WalkieConnectionState()
    data class Error(val message: String) : WalkieConnectionState()
}

data class HelicopterUiState(
    val usedAt: Instant? = null,
    val phase: HelicopterPhase = HelicopterPhase.IDLE,
    val notifyEndsAt: Instant? = null,
    val revealEndsAt: Instant? = null,
    val remainingMs: Long = 0L
)

enum class HelicopterPhase {
    IDLE,
    NOTIFY,
    REVEAL
}
