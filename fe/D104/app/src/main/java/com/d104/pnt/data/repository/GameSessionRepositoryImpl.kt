package com.d104.pnt.data.repository

import androidx.lifecycle.viewModelScope
import com.d104.pnt.base.Constants
import com.d104.pnt.data.remote.model.response.BeepUseResponse
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.remote.model.response.MemberLocationSocketDto
import com.d104.pnt.data.remote.model.response.Mission
import com.d104.pnt.data.remote.model.response.MissionSocketDto
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.service.game.GameActiveService
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.socket.GameSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameSessionRepositoryImpl @Inject constructor(
    private val gameSocketManager: GameSocketManager,
    private val stepSensorManager: StepSensorManager,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val imageRepository: ImageRepository,
    private val walkieRepository: WalkieRepository,
    private val gameRepository: GameRepository
) : GameSessionRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val WARNING_TIME = 5
    private val HELICOPTER_TIME = 5

    // 실시간 데이터를 저장할 메모리 공간
    private val _gameId = MutableStateFlow(0L)
    override val gameId = _gameId.asStateFlow()
    private val _gameStatus = MutableStateFlow("")
    override val gameStatus = _gameStatus.asStateFlow()

    private val _gameTime = MutableStateFlow(0)
    override val gameTime = _gameTime.asStateFlow()

    private val _missions = MutableStateFlow<List<MissionSocketDto>>(emptyList())
    override val missions = _missions.asStateFlow()
    private val _members = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    override val members = _members.asStateFlow()

    private val _memberLocation = MutableStateFlow<List<MemberLocationSocketDto>>(emptyList())
    override val memberLocation = _memberLocation.asStateFlow()

    override val thiefMembers = _members.map { list ->
        list.filter { it.position.equals("THIEF", ignoreCase = true) }
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _eventFlow = MutableSharedFlow<GameSessionEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val eventFlow = _eventFlow.asSharedFlow()

    private val _isOutOfBoundary = MutableStateFlow(false)
    override val isOutOfBoundary = _isOutOfBoundary.asStateFlow()

    private val _chiefMemberId = MutableStateFlow<Long?>(null)
    override val chiefMemberId = _chiefMemberId.asStateFlow()

    private val _missionState = MutableStateFlow(MissionStatus.IDLE)
    override val missionState = _missionState.asStateFlow()

    private val _missionFailReason = MutableStateFlow("")
    override val missionFailReason = _missionFailReason.asStateFlow()

    private var isConnecting = false

    private val _myMemberId = MutableStateFlow(0L)
    override val myMemberId = _myMemberId.asStateFlow()

    private val _myRole = MutableStateFlow("")
    override val myRole = _myRole.asStateFlow()

    private val _roomCode = MutableStateFlow("")
    override val roomCode = _roomCode.asStateFlow()

    private val _escapeQueue = MutableStateFlow<List<String>>(emptyList())
    override val escapeQueue = _escapeQueue.asStateFlow()

    private val _beepEvent = MutableSharedFlow<BeepUseResponse>(extraBufferCapacity = 16)
    override val beepEvent = _beepEvent.asSharedFlow()

    private val _baseTime = MutableStateFlow(0) // 탈옥 시 해당 시간으로 초기화
    private val _survivalTime = MutableStateFlow(0)
    override val survivalTime = _survivalTime.asStateFlow()

    private val _longestSurvivalTime = MutableStateFlow(0)
    override val longestSurvivalTime = _longestSurvivalTime.asStateFlow()

    private val _skillUsedAt = MutableStateFlow<Int?>(null)
    override val skillUsedAt = _skillUsedAt.asStateFlow()

    private val _helicopterUsed = MutableStateFlow(false)
    override val helicopterUsed: StateFlow<Boolean> = _helicopterUsed.asStateFlow()
    override val isChief: StateFlow<Boolean> =
        combine(myMemberId, chiefMemberId) { myId, chiefId ->
            chiefId != null && myId != 0L && myId == chiefId
        }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), false)


    override val helicopterButtonEnabled: StateFlow<Boolean> =
        combine(isChief, helicopterUsed) { chief, used ->
            chief && !used
        }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), false)
    private val _helicopterState = MutableStateFlow(HelicopterPhase.IDLE)
    override val helicopterState = _helicopterState.asStateFlow()

    // ===== 무전기 =====
    private val _walkieState = MutableStateFlow<WalkieConnectionState>(WalkieConnectionState.Idle)
    override val walkieState = _walkieState.asStateFlow()

    private val _isSomeoneTalking = MutableStateFlow(false)
    override val isSomeoneTalking = _isSomeoneTalking.asStateFlow()


    private val _isTransmitting = MutableStateFlow(false)
    override val isTransmitting = _isTransmitting.asStateFlow()

    private val _talkingMemberId = MutableStateFlow<Long?>(null)
    override val talkingMemberId = _talkingMemberId.asStateFlow()

    override fun setMemberId(memberId: Long) {
        _myMemberId.value = memberId
    }

    override fun setFinalRole(role: String) {
        _myRole.value = role
    }

    override fun setRoomCode(code: String) {
        _roomCode.value = code
        Timber.d("📍 Repository에 roomCode 저장 완료: $code")
    }

    private var gpsJob: Job? = null
    private var warningJob: Job? = null
    private var gameStartTime: Long = 0L
    private var radioTimeoutJob: Job? = null
    private var pttHeartbeatJob: Job? = null


    override fun gameInit() {
        repositoryScope.launch {
            setupSocketListeners()

            delay(500L)
            gameSocketManager.syncGameInfo()
        }
        stepSensorManager.startListening()
    }

    override fun connectAndJoin(gameId: Long) {
        if (isConnecting || gameSocketManager.isConnected()) return

        repositoryScope.launch {
            isConnecting = true

            try {
                // 1. 리스너부터 확실히 먼저 등록 (신호를 놓치지 않게)
                gameSocketManager.removeAllListeners()
                setupSocketListeners()

                // 2. 연결 시도
                val token = authRepository.getAccessToken().first()
                gameSocketManager.connect(token)

                // 3. 연결될 때까지 대기
                var retry = 0
                while (!gameSocketManager.isConnected() && retry < 30) {
                    delay(100)
                    retry++
                }

                if (gameSocketManager.isConnected()) {
                    // 4. 연결 성공 후 딱 한 번만 Join 요청
                    gameSocketManager.joinGame(gameId)
                }
            } finally {
                isConnecting = false
            }
        }
    }

    private fun setupSocketListeners() {
        gameSocketManager.setOnGameStarted { gameId, startTime ->
            _gameId.value = gameId
            repositoryScope.launch {
                // 뷰모델에게 "넘어가라"고 신호 보냄
                _eventFlow.emit(GameSessionEvent.GameStarted(gameId, startTime))
            }
        }
        gameSocketManager.setOnGpsReceived {cctvThiefId, skillUsedAt, sec, locations ->
            _gameTime.value = sec
            try {
                if (skillUsedAt != null && _skillUsedAt.value == null) {
                    _skillUsedAt.value = sec
                    _helicopterState.value = HelicopterPhase.NOTIFY
                }
                if (_helicopterState.value == HelicopterPhase.NOTIFY &&
                    sec >= _skillUsedAt.value!! + WARNING_TIME
                ) {
                    _helicopterState.value = HelicopterPhase.REVEAL
                }
                if (_helicopterState.value == HelicopterPhase.REVEAL &&
                    sec >= _skillUsedAt.value!! + WARNING_TIME + HELICOPTER_TIME
                ) {
                    _helicopterState.value = HelicopterPhase.IDLE
                }
            } catch (e: Exception) {
                _skillUsedAt.value = sec
                _helicopterState.value = HelicopterPhase.IDLE
            }
            try {
                if (locations.length() != 0) {
                    val newMemberLocation = mutableListOf<MemberLocationSocketDto>()
                    for (i in 0 until locations.length()) {
                        val locationJson = locations.getJSONObject(i)
                        if (cctvThiefId != null) {
                            if (locationJson.optLong("memberId") == cctvThiefId) {
                                locationJson.put("status", "CCTV")
                            }
                        }
                        if (_helicopterState.value == HelicopterPhase.REVEAL) {
                            locationJson.put("status", "CCTV")
                        }
                        newMemberLocation.add(MemberLocationSocketDto.fromJson(locationJson))
                    }
                    _memberLocation.value = newMemberLocation
                    Timber.d("멤버 위치 파싱 완료: ${newMemberLocation}")
                }
            }
            catch (e:Exception) {
                Timber.e(e, "멤버 위치 파싱 실패")
            }
        }

        gameSocketManager.setOnJoinedRoom { gameId, memberId, message ->
            _gameId.value = gameId
        }
        gameSocketManager.setOnWillStartGame { gameId, willStartAt ->
            Timber.d("socket ⏰ get will start game 수신 - gameId: $gameId, willStartAt: $willStartAt")
        }

        // 전체 게임 정보 동기화
        gameSocketManager.setOnGameInfoSynced { data ->
            try {
                val gameId = data.optLong("gameId")
                if (gameId != 0L) _gameId.value = gameId // ⭐ 저장

                val gameStatus = data.optString("gameStatus")
                if (gameStatus != "") _gameStatus.value = gameStatus

                val membersArray = data.optJSONArray("members")
                if (membersArray != null) {
                    val newMembers = mutableListOf<GameMemberSocketDto>()
                    for (i in 0 until membersArray.length()) {
                        val memberJson = membersArray.getJSONObject(i)
                        newMembers.add(GameMemberSocketDto.fromJson(memberJson))
                    }

                    _members.value = newMembers
                    Timber.d("GamePlayViewModel: 전체 멤버 동기화 완료 (${newMembers.size}명)")
                }
                val missionArray = data.optJSONArray("missions")
                if (missionArray != null) {
                    val newMissions = mutableListOf<MissionSocketDto>()
                    for (i in 0 until missionArray.length()) {
                        val missionJson = missionArray.getJSONObject(i)
                        newMissions.add(MissionSocketDto.fromJson(missionJson))
                    }
                    _missions.value = newMissions
                    Timber.d("GamePlayViewModel: 전체 미션 동기화 완료 (${newMissions})")
                }
            } catch (e: Exception) {
                Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
            }
        }

        // 실시간 상태 변경
        gameSocketManager.setOnMemberStatusChanged { gameId, thiefId, status, arrestedAt ->
            repositoryScope.launch {
                _members.update { currentList ->
                    currentList.map { member ->
                        if (member.memberId == thiefId) {
                            member.copy(rawStatus = status)
                        }
                        else member
                    }
                }
                Timber.d("GamePlayViewModel: 도둑($thiefId) 상태 변경 -> $status")
            }
        }

        // 도둑 탈출 수신
        gameSocketManager.setOnThiefEscaped { gameId, thiefId, escapedAt ->
            repositoryScope.launch {
                Timber.d("🏃 도둑 탈출 알림 수신: thiefId=$thiefId, escapedAt=$escapedAt")

                val escapedThief = members.value.find { it.memberId == thiefId }
                val thiefNickname = escapedThief?.nickname ?: "도둑"

                _escapeQueue.update {it + thiefNickname}
                Timber.d("📋 탈출 큐에 추가: $thiefNickname (현재 큐 크기: ${_escapeQueue.value.size})")
            }
        }

        // 비프음 수신 (GameSocketManager에서 이미 get beep use 파싱/콜백 호출 중)
        gameSocketManager.setOnBeepReceived { policeId, thiefId, distance ->
            _beepEvent.tryEmit(
                BeepUseResponse(
                    policeId = policeId,
                    thiefId = thiefId,
                    distance = distance
                )
            )
            Timber.d("📢 beep 수신: policeId=$policeId, thiefId=$thiefId, distance=$distance")
        }

        // 게임 종료 수신 -> 상세 결과 요청
        gameSocketManager.setOnGameEnded { winnerPosition, _ ->
            Timber.d("socket 🏁 게임 종료: $winnerPosition 승리 -> 상세 결과 요청")
            stopGameSession()
            gameSocketManager.postAfterGameEnd(_gameId.value)
        }

        // 상세 결과 수신 -> 이동 이벤트 발송
        gameSocketManager.setOnEndGameAfter { data ->
            repositoryScope.launch {
                _eventFlow.emit(GameSessionEvent.NavigateToLoading(_gameId.value))
            }
        }

        // 뉴스 생성 완료 수신
        gameSocketManager.setOnNewsReceived { gameId, newsId ->
            repositoryScope.launch {
                Timber.d("socket 📰 뉴스 도착 알림 수신: newsId=$newsId")
                _eventFlow.emit(GameSessionEvent.NavigateToNews(gameId, newsId))
            }
        }

        // 경게 벗어남 이벤트 수신 ->  경고 오버레이 띄움
        gameSocketManager.setOnOutOfBoundary { gameId, memberId ->
            Timber.w("⚠️ 경고: 구역 이탈 발생! (Game: $gameId)")
            showWarningEffect()
        }

        // 게임 미션 결과 수신 -> 성공 / 실패
        gameSocketManager.setOnMissionResult { gameId, missionId, thiefId, success, reason, completedAt ->
            if (success) {
                if (_myMemberId.value == thiefId) {
                    _missionState.value = MissionStatus.SUCCESS
                }
                val missionList = mutableListOf<MissionSocketDto>()
                _missions.value.forEach { mission ->
                    if (mission.id == missionId) {
                        val newMission = mission.copy(status = "SUCCESS")
                        missionList.add(newMission)
                    }
                    else { missionList.add(mission) }
                }
                _missions.value = missionList
                Timber.d("미션 목록 업데이트: $_missions.value")
            } else {
                if (_myMemberId.value == thiefId) {
                    _missionState.value = MissionStatus.FAIL
                    _missionFailReason.value = when (reason){
                        "ALREADY_COMPLETED" -> "다른 도둑에게 이 미션을 빼앗겼습니다!!"
                        "NOT_MATCHED" -> "목표를 찾지 못했습니다!"
                        else -> "사진을 인식하지 못했습니다!"
                    }
                }
                Timber.d("도둑 $thiefId 번 미션 실패!")
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

        gameSocketManager.setOnRadioReceived { _, memberId ->
            handleRadioSignal(memberId)
        }
    }

    override fun startGameSession() {
        if (gpsJob?.isActive == true) return

        gpsJob?.cancel()

        // 걸음 수 리셋
        stepSensorManager.resetGameSteps()
        gameStartTime = System.currentTimeMillis()

        gpsJob = repositoryScope.launch {
            // gameId가 0보다 커질 때까지 대기
            gameId.first { it > 0 }
            Timber.d("socket 🚀 레포지토리: GPS 전송 시작")
            while (isActive) {
                val location = locationRepository.currentLocation.value
                // 리셋된 걸음 수를 가져옴 (StepSensorManager에 gameStepCountFlow가 있다고 가정)
                val steps = stepSensorManager.stepCountFlow.value

                if (location != null) {

                    gameSocketManager.sendGPS(
                        lat = location.latitude,
                        lng = location.longitude,
                        walk = steps,
                        longestSurvived = _longestSurvivalTime.value
                    )
                    Timber.d("socket sendGPS: $location, $steps, $_longestSurvivalTime")
                }
                delay(1000L)
            }
        }
    }

    override fun uploadMissionImage(image: File, missionId: Long) {
        repositoryScope.launch {
            _missionState.value = MissionStatus.IN_ANALYZE
            delay(3000L)
            when (val result = imageRepository.getPresignedUrlToMission(image.name)) {
                is BaseResult.Success -> {
                    imageRepository.uploadImage(result.data.presignedUrl, image)
                    gameSocketManager.submitMissionImage(
                        authRepository.getMemberId().first(),
                        missionId,
                        result.data.downloadUrl
                    )
                }
                is BaseResult.Error -> {
                    Timber.e(result.error.message)
                }
            }
        }
    }

    override fun missionInit() {
        _missionState.value = MissionStatus.IDLE
    }

    // 서비스 종료 시 호출할 함수
    override fun stopGameSession() {
        gpsJob?.cancel()
        gpsJob = null
        gameStartTime = 0L
        stepSensorManager.stopListening()
        Timber.d("socket 레포지토리: GPS 전송 중단")
    }

    override fun leaveGame() {
        stopGameSession()

        gameSocketManager.leaveGame()

        _members.value = emptyList()
        _gameId.value = 0L
    }

    override fun setChiefMemberId(id: Long?) {
        _chiefMemberId.value = id
    }

    private fun showWarningEffect() {
        warningJob?.cancel()
        warningJob = repositoryScope.launch {
            _isOutOfBoundary.value = true
            delay(3000) // 3초간 유지
            _isOutOfBoundary.value = false
        }
    }

    override fun dequeEscape(){
        _escapeQueue.value = _escapeQueue.value.drop(1)
        Timber.d("📋 탈출 큐에서 제거 (남은 큐 크기: ${_escapeQueue.value.size})")
    }

    override fun connectWalkie() {
        // 경찰이 아니면 무시
        if (_myRole.value != "POLICE") {
            Timber.d("🎙️ 도둑은 무전기 연결 안함")
            return
        }

        repositoryScope.launch {
            try {
                _walkieState.value = WalkieConnectionState.Connecting

                // 1. roomCode 가져오기
                val roomCode = roomCode.first()
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

    override fun disconnectWalkie() {
        repositoryScope.launch {
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

        repositoryScope.launch {
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

        repositoryScope.launch {
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

    override fun startTalking() {
        if (_myRole.value != "POLICE") return

        repositoryScope.launch {
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

    override fun stopTalking() {
        if (_myRole.value != "POLICE") return

        repositoryScope.launch {
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

    override fun useHelicopterSkill() {
        // 청장만
        if (_myMemberId.value == 0L || _myMemberId.value != _chiefMemberId.value) {
            Timber.w("🚁 스킬 사용 불가: 청장 아님 (my=${_myMemberId.value}, chief=${_chiefMemberId.value})")
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
        gameSocketManager.useSkill(policeId = _myMemberId.value)
        Timber.d("🚁 post skill use 요청: gameId=$gameId, policeId=${_myMemberId.value}")
    }
}

sealed class GameSessionEvent {

    // 1. 게임 시작 신호 (게임 ID와 시작 시간을 담아서 보냄)
    data class GameStarted(
        val gameId: Long,
        val startTime: String
    ) : GameSessionEvent()

    // 2. (예시) 게임 종료 신호
    data object GameEnded : GameSessionEvent()

    // 3. (예시) 에러 발생 신호
    data class ErrorOccurred(val message: String) : GameSessionEvent()

    data class NavigateToLoading(val gameId: Long) : GameSessionEvent()
    data class NavigateToNews(val gameId: Long, val newsId: Long) : GameSessionEvent() // 실제 뉴스로 이동
}

sealed class WalkieConnectionState {
    object Idle : WalkieConnectionState()
    object Connecting : WalkieConnectionState()
    object Connected : WalkieConnectionState()
    data class Error(val message: String) : WalkieConnectionState()
}

enum class HelicopterPhase {
    IDLE,
    NOTIFY,
    REVEAL
}

enum class MissionStatus {
    IDLE, IN_ANALYZE, SUCCESS, FAIL
}